/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.impl;

import org.opendaylight.federation.service.api.IProducerSubscriptionMgr;
import org.opendaylight.federation.service.api.message.SubscribeMessage;
import org.opendaylight.federation.service.api.message.UnsubscribeMessage;
import org.opendaylight.messagequeue.AbstractFederationMessage;
import org.opendaylight.messagequeue.IGeneralFederationConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlMessagesConsumer implements IGeneralFederationConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(ControlMessagesConsumer.class);

    private final IProducerSubscriptionMgr producerMgr;

    public ControlMessagesConsumer(IProducerSubscriptionMgr producerMgr) {
        this.producerMgr = producerMgr;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void consumeMsg(AbstractFederationMessage msg) {
        LOG.info("consuming message {}", msg);
        if (msg instanceof SubscribeMessage) {
            producerMgr.handleSubscribeMsg((SubscribeMessage) msg);
        } else if (msg instanceof UnsubscribeMessage) {
            producerMgr.handleUnsubscribeMsg((UnsubscribeMessage) msg);
        }
    }

}
