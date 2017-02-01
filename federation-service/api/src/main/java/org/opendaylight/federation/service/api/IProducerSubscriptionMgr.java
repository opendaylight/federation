/*
 * Copyright (c) 2017 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.api;

import org.opendaylight.federation.service.api.message.SubscribeMessage;
import org.opendaylight.federation.service.api.message.UnsubscribeMessage;

/**
 * This interface is implemented by the egress side of the federation service infrastructure and is NOT exposed to the
 * egress plugins. It handles subscriptions from remote sites.
 */
public interface IProducerSubscriptionMgr {

    /**
     * Invoked as a result of an incoming {@link SubscribeMessage}. This will start a federation session between this
     * site and the subscriber site.
     *
     * @param msg The subscription message that was sent by the remote site.
     */
    void handleSubscribeMsg(SubscribeMessage msg);

    /**
     * Invoked as a result of an incoming {@link UnsubscribeMessage}. This will close a federation session between this
     * site and the remote site.
     *
     * @param msg The unsubscription message that was sent by the remote site.
     */
    void handleUnsubscribeMsg(UnsubscribeMessage msg);

}
