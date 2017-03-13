/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.impl;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.federation.plugin.spi.IFederationPluginEgress;
import org.opendaylight.federation.plugin.spi.IPluginFactory;
import org.opendaylight.federation.service.api.IConsumerManagement;
import org.opendaylight.federation.service.api.IFederationProducerMgr;
import org.opendaylight.federation.service.api.IProducerSubscriptionMgr;
import org.opendaylight.federation.service.api.federationutil.FederationConstants;
import org.opendaylight.federation.service.api.federationutil.FederationCounters;
import org.opendaylight.federation.service.api.message.EndFullSyncFederationMessage;
import org.opendaylight.federation.service.api.message.FullSyncFailedFederationMessage;
import org.opendaylight.federation.service.api.message.StartFullSyncFederationMessage;
import org.opendaylight.federation.service.api.message.SubscribeMessage;
import org.opendaylight.federation.service.api.message.UnsubscribeMessage;
import org.opendaylight.federation.service.api.message.WrapperEntityFederationMessage;
import org.opendaylight.federation.service.common.api.EntityFederationMessage;
import org.opendaylight.federation.service.common.api.ListenerData;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.messagequeue.IMessageBusClient;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.federation.service.config.rev161110.FederationConfigData;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings(value = { "checkstyle:illegalcatch" })
public class FederationProducerMgr
    implements IFederationProducerMgr, IProducerSubscriptionMgr, ClusterSingletonService {

    private final ConcurrentHashMap<String, ConsumerState> consumerIdToState = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, IPluginFactory> pluginTypeToFactory = new ConcurrentHashMap<>();
    private IMessageBusClient messageBus = null;
    private DataBroker db;
    private final FederationConfigData config;

    private static final Logger LOG = LoggerFactory.getLogger(FederationProducerMgr.class);
    private static final ServiceGroupIdentifier IDENT =
        ServiceGroupIdentifier.create(FederationConstants.CLUSTERING_SERVICE_ID);
    private volatile String controlQueueConsumerTag = null;
    private final ClusterSingletonServiceProvider clusterSingletonServiceProvider;
    private ClusterSingletonServiceRegistration clusterRegistrationHandle;
    private final ScheduledThreadPoolExecutor retryExecutor = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<?> retryHandle;
    private final IConsumerManagement consumerMgr;
    private static final int RETRY_INTERVAL = 10;

    public FederationProducerMgr(IMessageBusClient messageBus, DataBroker db, FederationConfigData config,
        ClusterSingletonServiceProvider clusterSingletonServiceProvider, IConsumerManagement consumerMgr) {
        this.messageBus = messageBus;
        this.db = db;
        this.config = config;
        this.clusterSingletonServiceProvider = clusterSingletonServiceProvider;
        this.consumerMgr = consumerMgr;
    }

    public void init() {
        LOG.info("starting {}", getClass().getSimpleName());
        if (config.isStartService()) {
            LOG.info("registering {} to cluster service", getClass().getSimpleName());
            clusterRegistrationHandle = clusterSingletonServiceProvider.registerClusterSingletonService(this);
        } else {
            LOG.info("Federation startService is configured to false");
        }
        storeConfigInDatastore();
    }

    public void destroy() {
        LOG.info("closing {}", getClass().getSimpleName());
        if (!config.isStartService()) {
            LOG.info("Nothing to do, because service wasn't actually configured to start (startService was false)");
            return;
        }
        try {
            if (clusterRegistrationHandle != null) {
                clusterRegistrationHandle.close();
            }
        } catch (Throwable t) {
            LOG.error("Couldn't unregister from cluster singleton service", t);
        }
        LOG.info("Destroying control queue {}", config.getControlQueueName());
        messageBus.destroyQueue(config.getControlQueueName());

        for (Entry<String, ConsumerState> entry : consumerIdToState.entrySet()) {
            String consumerId = entry.getKey();
            ConsumerState state = entry.getValue();
            LOG.info("Aborting and cleaning egress plugin of {}", consumerId);
            CompletionStage<Void> aborted = state.pluginEgress.abort();
            try {
                aborted.toCompletableFuture().get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOG.warn("Waited for plugin to abort for 5 seconds, but recieved {}", e.getMessage());
            }
            state.pluginEgress.cleanup();
            LOG.info("Destroying dynamic queue {}", state.dynamicQueueName);
            messageBus.destroyQueue(state.dynamicQueueName);
        }
    }

    public void setDataBroker(DataBroker dataBroker) {
        this.db = dataBroker;
    }

    public void setMessageBus(IMessageBusClient messageBus) {
        this.messageBus = messageBus;
    }

    @Override
    public synchronized void handleSubscribeMsg(SubscribeMessage msg) {
        IPluginFactory pluginFactory = pluginTypeToFactory.get(msg.getPluginType());
        if (pluginFactory == null) {
            LOG.error("Received Subscribe msg for a plugin that doesn't have a factory: {}", msg);
            return;
        }
        try {
            LOG.info("Unsubscribe former data of consumer if exists");
            unsubscribeConsumer(msg.getContextId());
            LOG.info("Create new consumer context");
            messageBus.createQueue(msg.getDynamicQueueName(), config.getMqBrokerIp(), config.getMqPortNumber(),
                config.getMqUser(), config.getMqUserPwd());
            ConsumerState consumerState = createConsumerContext(msg, pluginFactory);
            List<ListenerData> listenersData = consumerState.pluginEgress.getListenersData();
            publishStartFullSyncMsg(msg.getDynamicQueueName(), msg.getContextId());
            try {
                handleFullSync(consumerState, listenersData);
            } catch (Throwable t) {
                publishFullSyncFailedMsg(msg.getDynamicQueueName(), msg.getContextId());
                return;
            }
            publishEndFullSyncMsg(msg.getDynamicQueueName(), msg.getContextId());
            createSteadySyncListeners(consumerState, listenersData);
            if (msg.isRequestMutualSubscription()) {
                consumerMgr.triggerPluginResubscription(msg.getSubscriberIp());
            }
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void createSteadySyncListeners(ConsumerState consumerState, List<ListenerData> listenersData) {
        for (ListenerData data : listenersData) {
            FederationDataChangeListener listener =
                new FederationDataChangeListener(data.listenerId, consumerState.pluginEgress);
            ListenerRegistration registrationHandle = db.registerDataTreeChangeListener(data.listenerPath, listener);
            consumerState.registrationHandles.add(registrationHandle);
        }
    }

    private void handleFullSync(ConsumerState consumerState, List<ListenerData> listenersData)
        throws ReadFailedException {
        for (ListenerData data : listenersData) {
            notifyExistingData(consumerState.pluginEgress, data);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void notifyExistingData(IFederationPluginEgress pluginEgress, ListenerData data)
        throws ReadFailedException {
        ReadOnlyTransaction readTx = db.newReadOnlyTransaction();
        CheckedFuture<?, ReadFailedException> readFuture =
            readTx.read(data.checkExistingDataPath.getDatastoreType(), data.checkExistingDataPath.getRootIdentifier());
        Optional optionalExistingData = (Optional) readFuture.checkedGet();
        pluginEgress.fullSyncData(data.listenerId, optionalExistingData);
    }

    private ConsumerState createConsumerContext(SubscribeMessage msg, IPluginFactory pluginFactory) {
        IFederationPluginEgress pluginEgress =
            pluginFactory.createEgressPlugin(msg.getPayload(), msg.getDynamicQueueName(), msg.getContextId());
        ConsumerState consumerState = new ConsumerState(pluginEgress, msg.getDynamicQueueName());
        consumerIdToState.put(msg.getContextId(), consumerState);
        return consumerState;
    }

    @Override
    public synchronized void handleUnsubscribeMsg(UnsubscribeMessage msg) {
        if (!unsubscribeConsumer(msg.getContextId())) {
            LOG.error("Recieved unsubscribe message for a consumer that doesn't exist: {}", msg);
        }
    }

    private boolean unsubscribeConsumer(String consumerId) {
        boolean removed = true;
        ConsumerState state = consumerIdToState.get(consumerId);
        if (state != null) {
            CompletionStage<Void> aborted = state.pluginEgress.abort();
            try {
                aborted.toCompletableFuture().get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOG.warn("Waited for plugin to abort for 5 seconds, but recieved {}", e.getMessage());
            }
            for (ListenerRegistration<? extends DataObject> handle : state.registrationHandles) {
                LOG.info("Closed MD-SAL listener for {}", handle);
                handle.close();
            }
            state.pluginEgress.cleanup();
            try {
                messageBus.destroyQueue(state.dynamicQueueName);
            } catch (Throwable t) {
                LOG.warn("Destroying queue {} failed", t);
            }
            consumerIdToState.remove(consumerId);
        } else {
            LOG.info("No former data exists for consumer {}", consumerId);
            removed = false;
        }

        return removed;
    }

    private long getNextSequence(String consumerId) {
        return consumerIdToState.get(consumerId).generalSequence.getAndIncrement();
    }

    @Override
    public void publishMessage(EntityFederationMessage<? extends DataObject> msg, String queueName, String consumerId) {
        FederationCounters.msg_published.inc();
        WrapperEntityFederationMessage wrapperMsg =
            (WrapperEntityFederationMessage) new WrapperEntityFederationMessage(msg)
                .setSequenceId(getNextSequence(consumerId));
        messageBus.sendMsg(wrapperMsg, queueName);

    }

    @Override
    public void attachPluginFactory(String pluginType, IPluginFactory factory) {
        LOG.info("Attaching plugin type: {} to a factory", pluginType);
        pluginTypeToFactory.put(pluginType, factory);
    }

    @SuppressWarnings("rawtypes")
    private class FederationDataChangeListener implements ClusteredDataTreeChangeListener {

        private final IFederationPluginEgress pluginEgress;
        private final String listenerId;

        FederationDataChangeListener(String listenerId, IFederationPluginEgress pluginEgress) {
            this.listenerId = listenerId;
            this.pluginEgress = pluginEgress;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onDataTreeChanged(Collection changes) {
            FederationCounters.dcn.inc();
            pluginEgress.steadyData(listenerId, changes);
        }
    }

    private class ConsumerState {
        public AtomicLong generalSequence = new AtomicLong(1);
        public IFederationPluginEgress pluginEgress;
        public List<ListenerRegistration<? extends DataObject>> registrationHandles = new ArrayList<>();
        public String dynamicQueueName;

        ConsumerState(IFederationPluginEgress egressPlugin, String dynamicQueueName) {
            this.pluginEgress = egressPlugin;
            this.dynamicQueueName = dynamicQueueName;
        }
    }

    private void publishStartFullSyncMsg(String queueName, String consumerId) {
        LOG.info("Sent start full sync message to queue {} ", queueName);
        FederationCounters.start_full_sync_msg_sent.inc();
        StartFullSyncFederationMessage startFullSyncMsg = new StartFullSyncFederationMessage();
        messageBus.sendMsg(startFullSyncMsg, queueName);
    }

    private void publishFullSyncFailedMsg(String queueName, String consumerId) {
        LOG.info("Sent full sync failed message to queue {} ", queueName);
        FederationCounters.full_sync_failed_msg_sent.inc();
        FullSyncFailedFederationMessage fullSyncFailedMsg =
            new FullSyncFailedFederationMessage(getNextSequence(consumerId));
        messageBus.sendMsg(fullSyncFailedMsg, queueName);
    }

    private void publishEndFullSyncMsg(String queueName, String consumerId) {
        LOG.info("Sent end full sync message to queue {} ", queueName);
        FederationCounters.end_full_sync_msg_sent.inc();
        EndFullSyncFederationMessage endFullSyncMsg = new EndFullSyncFederationMessage(getNextSequence(consumerId));
        messageBus.sendMsg(endFullSyncMsg, queueName);
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return IDENT;
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        try {
            LOG.info("Lost federation leadership, detaching control queue handler");
            if (controlQueueConsumerTag != null) {
                messageBus.detachHandler(config.getControlQueueName(), controlQueueConsumerTag);
            }
        } catch (Throwable t) {
            LOG.error("Error while trying to lose leadership", t);
        }
        return Futures.immediateFuture(null);
    }

    @Override
    public void instantiateServiceInstance() {
        try {
            LOG.info("Gained federation leadership, creating control queue and attaching a handler");
            LOG.info("Latest configuration is: " + config);
            boolean created = messageBus.createQueue(config.getControlQueueName(), config.getMqBrokerIp(),
                config.getMqPortNumber(), config.getMqUser(), config.getMqUserPwd());
            if (!created) {
                LOG.error("Control queue wasn't created, scheduling retrier");
                retryHandle = retryExecutor.scheduleAtFixedRate(new RetryControlQueue(this), RETRY_INTERVAL,
                    RETRY_INTERVAL, TimeUnit.SECONDS);
            } else {
                ControlMessagesConsumer controlMessagesConsumer = new ControlMessagesConsumer(this);
                controlQueueConsumerTag =
                    messageBus.attachHandler(config.getControlQueueName(), controlMessagesConsumer);
                LOG.info("Control queue {} was created successfully", config.getControlQueueName());
            }
        } catch (Throwable t) {
            LOG.error("Error while trying to gain leadership", t);
        }
    }

    private class RetryControlQueue implements Runnable {

        private final FederationProducerMgr mgr;

        RetryControlQueue(FederationProducerMgr mgr) {
            this.mgr = mgr;
        }

        @Override
        public void run() {
            boolean created = messageBus.createQueue(config.getControlQueueName(), config.getMqBrokerIp(),
                config.getMqPortNumber(), config.getMqUser(), config.getMqUserPwd());
            if (!created) {
                LOG.error("Control queue wasn't created, retrying in {} seconds", RETRY_INTERVAL);
            } else {
                ControlMessagesConsumer controlMessagesConsumer = new ControlMessagesConsumer(mgr);
                controlQueueConsumerTag =
                    messageBus.attachHandler(config.getControlQueueName(), controlMessagesConsumer);
                LOG.info("Control queue was created successfully");
                retryHandle.cancel(true);
            }
        }
    }

    private void storeConfigInDatastore() {
        try {
            WriteTransaction writeTx = db.newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(FederationConfigData.class),
                config);
            writeTx.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.error("Couldn't commit FederationConfigData to MD-SAL", e);
        }
    }

    @Override
    public void detachPluginFactory(String pluginType) {
        LOG.info("Detaching the factory of plugin type: {} ", pluginType);
        pluginTypeToFactory.remove(pluginType);
    }
}
