/*
 * Copyright © 2016 HPE, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federationmessagequeue.impl;

import akka.osgi.BundleDelegatingClassLoader;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import org.objenesis.strategy.StdInstantiatorStrategy;
import org.opendaylight.messagequeue.AbstractFederationMessage;
import org.opendaylight.messagequeue.IGeneralFederationConsumer;
import org.opendaylight.messagequeue.IMessageBusClient;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings(value = { "checkstyle:illegalcatch" })
public class RabbitMessageBus implements IMessageBusClient {

    private static final Logger LOG = LoggerFactory.getLogger(RabbitMessageBus.class);
    private final Map<String, MessageBusConnectionData> queueNameToConnectionData = new ConcurrentHashMap<>();

    public RabbitMessageBus() {
    }

    @Override
    public boolean createQueue(String queueName, String brokerIp) {
        return createQueue(queueName, brokerIp, 5672, "guest", "guest");
    }

    /*
     * TODO If we already have a connection to the broker, reuse the existing connection instead of creating a new one
     */
    @Override
    public boolean createQueue(String queueName, String mqBrokerIp, int mqPortNumber, String mqUser, String mqUserPwd) {
        LOG.info("Creating connection for queue {} on broker {}", queueName, mqBrokerIp);

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(mqBrokerIp);
        factory.setPort(mqPortNumber);
        factory.setUsername(mqUser);
        factory.setPassword(mqUserPwd);
        factory.setAutomaticRecoveryEnabled(true);

        try {
            Connection connection = factory.newConnection();
            LOG.info("Created connection to broker {}:{} for user {} ", mqBrokerIp, mqPortNumber, mqUser);
            Channel channel = connection.createChannel();
            channel.queueDeclare(queueName, false, false, false, null);
            LOG.info("Declared queue {} on broker {}", queueName, mqBrokerIp);
            MessageBusConnectionData mbcd = new MessageBusConnectionData(mqBrokerIp, connection, channel);
            queueNameToConnectionData.put(queueName, mbcd);
            return true;
        } catch (IOException | TimeoutException e) {
            LOG.warn("Failed creating queue {} on broker {}:{} for user {} because: {}", queueName, mqBrokerIp,
                mqPortNumber, mqUser, e.getMessage());
            return false;
        }
    }

    public void init() {
        LOG.info("starting {}", getClass().getSimpleName());
    }

    public void close() {
        LOG.info("closing {}", getClass().getSimpleName());
    }

    @Override
    public void destroyQueue(String queueName) {
        LOG.info("Started delete of queue {}", queueName);

        // lookup connection by queueName
        MessageBusConnectionData messageBusConnectionData = queueNameToConnectionData.get(queueName);
        if (messageBusConnectionData != null) {
            // get channel from active connections map
            Channel channel = messageBusConnectionData.channel;
            String brokerIp = messageBusConnectionData.brokerIp;
            Connection conn = messageBusConnectionData.conn;
            try {
                // kill the queue dont wait for confirmation
                if (channel != null) {
                    try {
                        channel.queueDelete(queueName);
                        LOG.info("Deleted queue {} successfully", queueName);
                    } catch (IOException e) {
                        LOG.warn("Failed to delete queue {} msg: {}", queueName, e.getMessage());
                    }
                    channel.close();
                } else {
                    LOG.warn("Null channel while deleting queue {} on broker {}", queueName, brokerIp);
                }
                if (conn != null) {
                    conn.close();
                } else {
                    LOG.warn("Null connection while deleting queue {} on broker {}", queueName, brokerIp);
                }
            } catch (IOException | TimeoutException e) {
                LOG.warn("Failed to close channel while deleting queue {} on broker {}", queueName, brokerIp, e);
            }
            // remove the queue from the internal queue list
            queueNameToConnectionData.remove(queueName);
        } else {
            LOG.warn("Cancelled deletion of queue {} because queueName not found in queueNameToConnectionData",
                queueName);
        }

    }

    @SuppressWarnings(value = { "checkstyle:illegalcatch" })
    @Override
    public String attachHandler(String queueName, IGeneralFederationConsumer consumer) {
        MessageBusConnectionData messageBusConnectionData = queueNameToConnectionData.get(queueName);
        if (messageBusConnectionData != null) {

            Channel channel = messageBusConnectionData.channel;
            Consumer mqConsumer = createRabbitConsumer(consumer, channel);

            try {
                // start consuming from queue
                return channel.basicConsume(queueName, true, mqConsumer);
            } catch (IOException e) {
                String brokerIp = messageBusConnectionData.brokerIp;
                LOG.warn("Failed to consume from queue {} on broker {}", queueName, brokerIp, e);
            }

        } else {
            LOG.warn("AttachHandler failed - queue {} not found in the active connection map}", queueName);
        }
        return null;
    }

    private Consumer createRabbitConsumer(IGeneralFederationConsumer consumer, Channel channel) {
        Consumer mqConsumer = new DefaultConsumer(channel) {

            /*
             * The methods of this interface are invoked in a dispatch thread which is separate from the Connection's
             * thread. The Consumers on a particular Channel are invoked serially on one or more dispatch threads.
             */
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                byte[] body) throws IOException {

                Kryo kryo = new Kryo();
                kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

                Input input = new Input(new ByteArrayInputStream(body));
                try {
                    Bundle bundle = FrameworkUtil.getBundle(RabbitMessageBus.class);
                    BundleContext bundleContext = bundle.getBundleContext();
                    BundleDelegatingClassLoader loader = new BundleDelegatingClassLoader(bundleContext.getBundle(),
                        Thread.currentThread().getContextClassLoader());

                    kryo.setClassLoader(loader);
                    Object readObject = kryo.readClassAndObject(input);
                    if (readObject instanceof AbstractFederationMessage) {
                        RabbitCounters.received_msg.inc();
                        consumer.consumeMsg((AbstractFederationMessage) readObject);
                    } else {
                        LOG.error("Received an object not of type AbstractFederationMessage, type was: {}",
                            readObject.getClass().getName());
                    }
                    LOG.trace("Deserialized {}", readObject);
                } catch (Throwable e) {
                    LOG.error("Failed in readObject: " + e.getMessage(), e);
                    return;
                }

            }
        };
        return mqConsumer;
    }

    @Override
    public synchronized void sendMsg(AbstractFederationMessage msg, String queueName) {

        // lookup connection by queueName
        MessageBusConnectionData messageBusConnectionData = queueNameToConnectionData.get(queueName);
        if (messageBusConnectionData != null) {
            Channel channel = messageBusConnectionData.channel;
            LOG.trace("Sending msg to queue {}, msg {}", queueName, msg);

            // make sure that the queue is there (nothing happens if the
            // receiving side already created it
            createQueueIfNeeded(queueName, messageBusConnectionData, channel);
            byte[] byteArray = serializeUsingKryo(msg);

            try {
                channel.basicPublish("", queueName, null, byteArray);
                RabbitCounters.sent_msg.inc();
                LOG.debug("Sent msg to {} on broker {}", queueName, messageBusConnectionData.brokerIp);
            } catch (IOException e) {
                LOG.error("Failed to send message to queue {} on broker {} because {}", queueName,
                    messageBusConnectionData.brokerIp, e.getMessage());
            }
        } else {
            LOG.error("sendMsg - unknown queue name {}", queueName);
            LOG.trace("Dropped msg {}", msg);
        }
    }

    private void createQueueIfNeeded(String queueName, MessageBusConnectionData messageBusConnectionData,
        Channel channel) {
        try {
            channel.queueDeclare(queueName, false, false, false, null);
        } catch (IOException e) {
            LOG.warn("Failed to declare queue {} on broker {}", queueName, messageBusConnectionData.brokerIp, e);
        }
    }

    private byte[] serializeUsingKryo(AbstractFederationMessage msg) {
        Kryo kryo = new Kryo();
        ((Kryo.DefaultInstantiatorStrategy) kryo.getInstantiatorStrategy())
            .setFallbackInstantiatorStrategy(new StdInstantiatorStrategy());
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Output output = new Output(stream);
        kryo.writeClassAndObject(output, msg);
        output.close();
        return stream.toByteArray();
    }

    private static class MessageBusConnectionData {
        public String brokerIp;
        public Connection conn;
        public Channel channel;

        MessageBusConnectionData(String brokerIp, Connection conn, Channel channel) {
            this.brokerIp = brokerIp;
            this.conn = conn;
            this.channel = channel;
        }
    }

    @Override
    public void detachHandler(String queueName, String consumerTag) {
        MessageBusConnectionData messageBusConnectionData = queueNameToConnectionData.get(queueName);
        if (messageBusConnectionData != null) {
            Channel channel = messageBusConnectionData.channel;
            try {
                LOG.info("Cancelling queue handler {} " + consumerTag);
                channel.basicCancel(consumerTag);
            } catch (IOException e) {
                LOG.error("Detaching queue handler failed", e);
            }
        } else {
            LOG.warn("unknown queue name {} couldn't detach handler", queueName);
        }
    }
}
