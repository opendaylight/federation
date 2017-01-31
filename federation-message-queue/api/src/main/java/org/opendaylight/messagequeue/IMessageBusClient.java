/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.messagequeue;

/**
 * Need to decide if this is internal to the federation service, or exposed completely to ODL.
 */
public interface IMessageBusClient {

    void destroyQueue(String queueName);

    String attachHandler(String queueName, IGeneralFederationConsumer consumer);

    void detachHandler(String queueName, String handlerTag);

    void sendMsg(AbstractFederationMessage msg, String queueName);

    boolean createQueue(String queueName, String mqBrokerIp, int mqPortNumber, String mqUser, String mqUserPwd);

    boolean createQueue(String queueName, String brokerIp);
}
