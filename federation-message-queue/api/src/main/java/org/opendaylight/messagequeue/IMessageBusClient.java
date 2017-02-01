/*
 * Copyright (c) 2017 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.messagequeue;

/**
 * The message queue library API which is consumed internally by the federation service infrastructure in order to
 * facilitate the low level queuing and messaging between sites.
 */
public interface IMessageBusClient {

    void destroyQueue(String queueName);

    String attachHandler(String queueName, IGeneralFederationConsumer consumer);

    void detachHandler(String queueName, String handlerTag);

    void sendMsg(AbstractFederationMessage msg, String queueName);

    boolean createQueue(String queueName, String mqBrokerIp, int mqPortNumber, String mqUser, String mqUserPwd);

    boolean createQueue(String queueName, String brokerIp);
}
