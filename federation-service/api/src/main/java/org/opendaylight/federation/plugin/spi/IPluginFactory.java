/*
 * Copyright (c) 2017 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.plugin.spi;

import java.io.Serializable;
import org.opendaylight.federation.service.api.IFederationConsumerMgr;
import org.opendaylight.federation.service.api.IFederationProducerMgr;
import org.opendaylight.federation.service.api.message.SubscribeMessage;

/**
 * Factory supplied as part of the SPI to create the {@link IFederationPluginEgress}.
 */
public interface IPluginFactory {

    /**
     * Invoked by the {@link IFederationProducerMgr} as a result of processing a {@link SubscribeMessage} that was
     * received from a remote site.
     *
     * @param payload The {@link Serializable} payload that was passed by the remote consumer site as part of
     *            {@link IFederationConsumerMgr#subscribe(String, Object, IFederationPluginIngress)}.
     * @param queueName Queue name that was precreated by the federation infrastructure in the consumer site for
     *            receiving messages and should be used when the plugin invokes
     *            {@link IFederationProducerMgr#publishMessage
     *            (org.opendaylight.federation.service.common.api.EntityFederationMessage, String, String)}.
     * @param contextId Context id that was precreated by the federation infrastructure in the consumer site for
     *            receiving messages and should be used when the plugin invokes
     *            {@link IFederationProducerMgr#publishMessage
     *            (org.opendaylight.federation.service.common.api.EntityFederationMessage, String, String)}.
     *
     * @return A new instance for hanlding the egress side logic of this federation session.
     */
    IFederationPluginEgress createEgressPlugin(Object payload, String queueName, String contextId);
}
