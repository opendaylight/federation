/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.federation.plugin.spi.IFederationPluginIngress;
import org.opendaylight.federation.service.api.IConsumerManagement;
import org.opendaylight.federation.service.api.IFederationConsumerMgr;
import org.opendaylight.federation.service.api.federationutil.FederationConstants;
import org.opendaylight.federation.service.api.federationutil.FederationCounters;
import org.opendaylight.federation.service.api.federationutil.FederationUtils;
import org.opendaylight.federation.service.api.message.SubscribeMessage;
import org.opendaylight.federation.service.api.message.UnsubscribeMessage;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.messagequeue.IMessageBusClient;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.federation.service.config.rev161110.FederationConfigData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.federation.service.config.rev161110.FederationSitesConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.federation.service.config.rev161110.federation.sites.config.FederationSiteConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.federation.service.config.rev161110.federation.sites.config.FederationSiteConfigKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings(value = { "checkstyle:illegalcatch" })
public class FederationConsumerMgr implements IFederationConsumerMgr, IConsumerManagement, ClusterSingletonService {

    private static final Logger LOG = LoggerFactory.getLogger(FederationConsumerMgr.class);

    private final IMessageBusClient messageBus;
    private final Map<String, RemoteSiteData> remoteIpToPluginConsumer = new ConcurrentHashMap<>();
    private final FederationConfigData config;
    private final DataBroker db;
    private final ClusterSingletonServiceProvider clusterSingletonServiceProvider;
    private volatile boolean isLeader = false;

    private static final ServiceGroupIdentifier IDENT =
            ServiceGroupIdentifier.create(FederationConstants.CLUSTERING_SERVICE_ID);

    private ClusterSingletonServiceRegistration clusterRegistrationHandle;

    public FederationConsumerMgr(IMessageBusClient messageBus, FederationConfigData config, DataBroker db,
            ClusterSingletonServiceProvider clusterSingletonServiceProvider) {
        this.messageBus = messageBus;
        this.config = config;
        this.db = db;
        this.clusterSingletonServiceProvider = clusterSingletonServiceProvider;
    }

    public void init() {
        LOG.info("starting {}", getClass().getSimpleName());
        if (config.isStartService()) {
            LOG.info("registering {} to cluster service", getClass().getSimpleName());
            clusterRegistrationHandle = clusterSingletonServiceProvider.registerClusterSingletonService(this);
        }
    }

    public void close() {
        LOG.info("closing {}", getClass().getSimpleName());
        if (clusterRegistrationHandle != null) {
            try {
                clusterRegistrationHandle.close();
            } catch (Throwable t) {
                LOG.error("Couldn't unregister from cluster singleton service", t);
            }
        }
    }

    @Override
    public synchronized void subscribe(String remoteIp, Object payload, IFederationPluginIngress pluginConsumer) {
        Preconditions.checkState(config.isStartService(), "Federation service is configured not to start");
        subscribe(remoteIp, payload, pluginConsumer, false);
    }

    @Override
    @SuppressWarnings("checkstyle:illegalCatch")
    public synchronized void subscribe(String remoteIp, Object payload, IFederationPluginIngress pluginConsumer,
            boolean requestMutualSubscription) {
        if (!isLeader) {
            LOG.error("Subscribe called on a non-leader service");
            return;
        }
        LOG.info("subscribe remote ip {}, payload {}", remoteIp, payload);

        String dynamicQueueName = FederationUtils.createDynamicQueueName(remoteIp, config.getSiteIp());
        ReadOnlyTransaction readTx = db.newReadOnlyTransaction();
        KeyedInstanceIdentifier<FederationSiteConfig, FederationSiteConfigKey> path =
                InstanceIdentifier.create(FederationSitesConfig.class).child(FederationSiteConfig.class,
                        new FederationSiteConfigKey(remoteIp));
        try {
            Optional<FederationSiteConfig> producerConfigOptional =
                    readTx.read(LogicalDatastoreType.CONFIGURATION, path).checkedGet();
            if (producerConfigOptional.isPresent()) {
                FederationSiteConfig producerConfig = producerConfigOptional.get();
                LOG.info("creating remote control queue to {}", remoteIp);
                messageBus.createQueue(producerConfig.getControlQueueName(), producerConfig.getBrokerIp(),
                        producerConfig.getMqPortNumber(), producerConfig.getMqUser(), producerConfig.getMqUserPwd());
                FederationCounters.remote_control_queue_created.inc();
                try {
                    RemoteSiteData previousSiteData = remoteIpToPluginConsumer.get(remoteIp);
                    if (previousSiteData != null) {
                        messageBus.destroyQueue(previousSiteData.dynamicQueueName);
                        FederationCounters.dynamic_queue_destroyed.inc();
                    }
                } catch (Exception e) {
                    LOG.warn("failed destroying dynaminc queue to remote ip " + remoteIp, e);
                }
                messageBus.createQueue(dynamicQueueName, producerConfig.getBrokerIp(), producerConfig.getMqPortNumber(),
                        producerConfig.getMqUser(), producerConfig.getMqUserPwd());
                FederationCounters.dynamic_queue_created.inc();
                RemoteSiteData siteData = new RemoteSiteData(pluginConsumer, dynamicQueueName);
                remoteIpToPluginConsumer.put(remoteIp, siteData);
                WrapperConsumer wc = new WrapperConsumer(remoteIp, pluginConsumer);
                messageBus.attachHandler(dynamicQueueName, wc);
                SubscribeMessage subscribeMessage =
                        new SubscribeMessage(dynamicQueueName, pluginConsumer.getPluginType(), payload,
                                config.getSiteIp(), config.getSiteIp(), requestMutualSubscription);
                messageBus.sendMsg(subscribeMessage, producerConfig.getControlQueueName());
                FederationCounters.subscription_message_sent.inc();
            } else {
                LOG.error("Producer config wasn't found for site {} ", remoteIp);
            }
        } catch (ReadFailedException e) {
            LOG.error("Producer config wasn't found for site " + remoteIp, e);
            return;
        }
    }

    @Override
    public void unsubscribe(String remoteIp) {
        Preconditions.checkState(config.isStartService(), "Federation service is configured not to start");
        if (!isLeader) {
            LOG.error("Unsubscribe called on a non-leader service");
            return;
        }
        RemoteSiteData siteData = remoteIpToPluginConsumer.remove(remoteIp);
        if (siteData != null) {
            LOG.info("unsubscribe remote ip: {}, queue: {} ", remoteIp, siteData.dynamicQueueName);
            UnsubscribeMessage unsubMsg = new UnsubscribeMessage(siteData.dynamicQueueName,
                    siteData.pluginIngress.getPluginType(), config.getSiteIp(), null);

            ReadOnlyTransaction readTx = db.newReadOnlyTransaction();
            KeyedInstanceIdentifier<FederationSiteConfig, FederationSiteConfigKey> path =
                    InstanceIdentifier.create(FederationSitesConfig.class).child(FederationSiteConfig.class,
                            new FederationSiteConfigKey(remoteIp));
            try {
                Optional<FederationSiteConfig> producerConfigOptional =
                        readTx.read(LogicalDatastoreType.CONFIGURATION, path).checkedGet();
                if (producerConfigOptional.isPresent()) {
                    FederationSiteConfig producerConfig = producerConfigOptional.get();
                    messageBus.sendMsg(unsubMsg, producerConfig.getControlQueueName());
                    FederationCounters.unsubscription_message_sent.inc();
                    messageBus.destroyQueue(siteData.dynamicQueueName);
                } else {
                    LOG.error("Producer config wasn't found for site {} ", remoteIp);
                    return;
                }
            } catch (Exception e) {
                LOG.error("Producer config wasn't found for site {} ", remoteIp, e);
                return;
            }
        } else {
            LOG.warn("trying to unsubscribe a non registered remote ip {}", remoteIp);
        }
    }

    private static class RemoteSiteData {
        IFederationPluginIngress pluginIngress;
        String dynamicQueueName;

        RemoteSiteData(IFederationPluginIngress pluginIngress, String dynamicQueueName) {
            this.pluginIngress = pluginIngress;
            this.dynamicQueueName = dynamicQueueName;
        }
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return IDENT;
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        isLeader = false;
        LOG.info("Lost federation leadership.");
        return Futures.immediateFuture(null);
    }

    @Override
    public void instantiateServiceInstance() {
        isLeader = true;
        LOG.info("Gained federation leadership.");
    }

    @Override
    public void triggerPluginResubscription(String remoteIp) {
        RemoteSiteData remoteSiteData = remoteIpToPluginConsumer.get(remoteIp);
        if (remoteSiteData != null) {
            LOG.info("Triggering plugin resubscription for remote IP {}", remoteIp);
            remoteSiteData.pluginIngress.resubscribe();
        } else {
            LOG.error("Request to trigger plugin resubscription for remoteIP {} but data not found", remoteIp);
        }
    }
}
