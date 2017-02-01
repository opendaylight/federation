/*
 * Copyright (c) 2017 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.api;

import org.opendaylight.federation.service.api.message.SubscribeMessage;

/**
 * This is the API invoked by the {@link IFederationProducerMgr} which allows it to pass resubscription requests to the
 * local site.
 */
public interface IConsumerManagement {

    /**
     * Can be invoked by the {@link IFederationProducerMgr} when a {@link SubscribeMessage} was received with a flag
     * indicating the remote site requests this site to resubscribe to him. This can happen when the remote site
     * suspects that this site might be missing some data, and the resubscription which will eventually call Full Sync,
     * would synchronize this state.
     *
     * @param remoteIp The ip of the remote site requesting resubscription.
     */
    void triggerPluginResubscription(String remoteIp);
}
