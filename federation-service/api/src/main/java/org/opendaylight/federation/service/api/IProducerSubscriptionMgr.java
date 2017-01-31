/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.api;

import org.opendaylight.federation.service.api.message.SubscribeMessage;
import org.opendaylight.federation.service.api.message.UnsubscribeMessage;

public interface IProducerSubscriptionMgr {
    @SuppressWarnings("rawtypes")
    void handleSubscribeMsg(SubscribeMessage msg);

    @SuppressWarnings("rawtypes")
    void handleUnsubscribeMsg(UnsubscribeMessage msg);

}
