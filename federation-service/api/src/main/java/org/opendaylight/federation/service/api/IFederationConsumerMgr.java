/*
 * Copyright (c) 2017 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.api;

import java.io.Serializable;
import org.opendaylight.federation.plugin.spi.IFederationPluginEgress;
import org.opendaylight.federation.plugin.spi.IFederationPluginIngress;
import org.opendaylight.federation.service.api.message.SubscribeMessage;

/**
 * The ingress side of the federation service infrastructure. This component facilitates the actual subscription by this
 * site to the remote site.
 */
public interface IFederationConsumerMgr {

    /**
     * Connect this site to a remote site for federation purposes. The subscription is always BY a consumer TO a
     * producer, which means that the local site asks the remote site to be the producer. To handle the opposite role,
     * the remote site must invoke a subscription to this site. The invocation of this method eventually sends a
     * {@link SubscribeMessage} to the remote site.
     *
     * @param remoteIp The ip of the remote producer site.
     * @param payload A {@link Serializable} payload that will be passed to the {@link IFederationPluginEgress} on the
     *            producer site.
     * @param pluginConsumer An instance of the {@link IFederationPluginIngress} to use in the context of this
     *            connection.
     */
    void subscribe(String remoteIp, Object payload, IFederationPluginIngress pluginConsumer);

    /**
     * Connect this site to a remote site for federation purposes. The subscription is always BY a consumer TO a
     * producer, which means that the local site asks the remote site to be the producer. To handle the opposite role,
     * the remote site must invoke a subscription to this site. Using requestMutualSubscription flag is only effective
     * if the other site is already subscribed to this site. It should be passed as true only in case the local site
     * suspects the remote site might have unsynchronized state, and it this will cause a resubscription and a return to
     * Full Sync stage. The invocation of this method eventually sends a {@link SubscribeMessage} to the remote site.
     *
     * @param remoteIp The ip of the remote producer site.
     * @param payload A {@link Serializable} payload that will be passed to the {@link IFederationPluginEgress} on the
     *            producer site.
     * @param pluginConsumer An instance of the {@link IFederationPluginIngress} to use in the context of this
     *            connection.
     * @param requestMutualSubscription A flag indicating if the remote site should resubscribe to this site. Only
     *            effective if the remote site has already subscribed to this site.
     */
    void subscribe(String remoteIp, Object payload, IFederationPluginIngress pluginConsumer,
        boolean requestMutualSubscription);

    /**
     * Unsubscribe from a remote producer site.
     *
     * @param remoteIp The ip of the remote producer site.
     */
    void unsubscribe(String remoteIp);

}
