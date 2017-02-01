/*
 * Copyright (c) 2017 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.plugin.spi;

import java.util.concurrent.CompletionStage;
import org.opendaylight.federation.service.api.IFederationProducerMgr;
import org.opendaylight.federation.service.common.api.EntityFederationMessage;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * This is the Ingress part of the SPI that needs to be implemented by a plugin that wishes to use the federation
 * service.
 */
public interface IFederationPluginIngress {

    /**
     * Invoked in the beginning of the Full Sync stage in order to let the plugin know that the subsequent messages are
     * part of the existing data of the remote site. Some applications might be order-sensitive in the way they can
     * merge entities into MD-SAL, and this indication gives them a chance to collect all subsequent messages, order
     * them, and then merge them into the MD-SAL in the correct order.
     */
    void beginFullSync();

    /**
     * Invoked in the end of the Full Sync stage in order to let the plugin know that all the existing data from the
     * remote site was sent, and subsequent messages will be part of the ongoing MD-SAL updates.
     */
    void endFullSync();

    /**
     * Invoked by the federation service when a message was received from a remote site. Because applications may be
     * order sensitive, this method will never be invoked concurrently.
     *
     * @param msg The message from the remote site containing a single {@link DataObject}.
     */
    void consumeMsg(EntityFederationMessage<? extends DataObject> msg);

    /**
     * This is a way for the federation service to ask the plugin to destroy itself and resubscribe to the service. A
     * resubscription that will eventually cause a new Full Sync stage. This method can be invoked by the service when
     * it identifies a potential data corruption. Another reason is a remote site that identifies some error state, and
     * can request the local site to resubscribe for Full Sync purposes.
     */
    void resubscribe();

    /**
     * The unique identifier of the plugin type. This identifier has to be the same identifier which is used at the
     * producer site to create the Egress Plugin using the {@link IPluginFactory} in the context of
     * {@link IFederationProducerMgr#attachPluginFactory(String, IPluginFactory)}.
     *
     * @return The identifier.
     */
    String getPluginType();

    /**
     * Can be invoked by the federation service in order to cause the plugin to stop processing. After the returned
     * {@link CompletionStage} is ready, it is expected that the plugin will not agree to process any further messages.
     *
     * @return A {@link CompletionStage} indicating the abort() work has completed.
     */
    CompletionStage<Void> abort();
}
