/*
 * Copyright (c) 2017 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.api;

import org.opendaylight.federation.plugin.spi.IFederationPluginEgress;
import org.opendaylight.federation.plugin.spi.IFederationPluginIngress;
import org.opendaylight.federation.plugin.spi.IPluginFactory;
import org.opendaylight.federation.service.api.message.SubscribeMessage;
import org.opendaylight.federation.service.common.api.EntityFederationMessage;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * The egress side of the federation service infrastructure. This component expects {@link IPluginFactory} to be
 * attached to it before processing any {@link SubscribeMessage} from remote sites. It also exposes the API for
 * publishing messages for the {@link IFederationPluginEgress}.
 */
public interface IFederationProducerMgr {

    /**
     * Invoked by the {@link IFederationPluginEgress} when it wishes to send an {@link EntityFederationMessage} to a
     * remote site.
     *
     * @param msg The message to be sent.
     * @param queueName The queue name of the remote site which was precreated by the federation service, and passed
     *            as part of the {@link IPluginFactory#createEgressPlugin(Object, String, String)}.
     * @param contextId The context id of the remote site which was precreted by the federation service, and passed as
     *            part of the {@link IPluginFactory#createEgressPlugin(Object, String, String)}.
     */
    void publishMessage(EntityFederationMessage<? extends DataObject> msg, String queueName,
        String contextId);

    /**
     * Attaching a plugin factory MUST happen before a remote site subscribes to this site.
     * {@link IFederationPluginEgress} are created as a result of this service processing a {@link SubscribeMessage} and
     * eventually invoking the {@link IPluginFactory#createEgressPlugin(Object, String, String)}.
     *
     * @param pluginType A unique identifier for the plugin type. This identifier should be returned by the
     *            {@link IFederationPluginIngress#getPluginType()} when subscribing from the remote site.
     * @param factory The factory that creates the {@link IFederationPluginEgress}.
     */
    void attachPluginFactory(String pluginType, IPluginFactory factory);

    /**
     * Detaches a plugin factory.
     *
     * @param pluginType The identifier of the plugin factory to detach.
     */
    void detachPluginFactory(String pluginType);
}
