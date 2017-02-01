/*
 * Copyright (c) 2017 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.plugin.spi;

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionStage;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.federation.service.api.IFederationProducerMgr;
import org.opendaylight.federation.service.common.api.ListenerData;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * This is the Egress part of the SPI that needs to be implemented by a plugin that wishes to use the federation
 * service.
 */
public interface IFederationPluginEgress {

    /**
     * This method is invoked during the Steady Sync stage, upon a onDataChange() notification from MD-SAL. It contains
     * the data that was received in the notification for the specific DataTreeIdentifier. In the context of this
     * method, the plugin may decide to send entity messages to a remote site by invoking {@link IFederationProducerMgr}
     * publishMessage().
     *
     * @param listenerKey The identifier that exists in each {@link ListenerData} instance that was returned as part
     *            of the {@link IFederationPluginEgress#getListenersData()} invocation.
     * @param data The collection of {@link DataTreeModification} that was received in the MD-SAL notification.
     */
    void steadyData(String listenerKey, Collection<DataTreeModification<? extends DataObject>> data);

    /**
     * This method is invoked during the Full Sync stage, once per each type of entity. It contains all the existing
     * data in the MD-SAL for the specific DataTreeIdentifier. In the context of this method, the plugin may decide to
     * send entity messages to a remote site by invoking {@link IFederationProducerMgr} publishMessage().
     *
     * @param listenerKey The identifier that exists in each {@link ListenerData} instance that was returned as part
     *            of the {@link IFederationPluginEgress#getListenersData()} invocation.
     * @param existingData The {@link DataObject} that was read from the MD-SAL according to the given
     *            {@link org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier}.
     */
    void fullSyncData(String listenerKey, Optional<? extends DataObject> existingData);

    /**
     * The collection of all relevant DataTreeIdentifier entities that interest this plugin.
     */

    /**
     * Invoked by the federation service to learn the entities that the plugin is interested in.
     *
     * @return A list that describes all the {@link org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier}
     *         that the plugin wishes to listen on for federation purposes. These identifiers will be used later in the
     *         {@link IFederationPluginEgress#fullSyncData(String, Optional)} and
     *         {@link IFederationPluginEgress#steadyData(String, Collection)} invocations.
     */
    List<ListenerData> getListenersData();

    /**
     * Invoked after {@link IFederationPluginEgress#abort()} to allow the plugin to do resource cleaning logic.
     */
    void cleanup();

    /**
     * Invoked by the federation service in context of unsubscription. After the returned {@link CompletionStage} is
     * ready, it is expected that the plugin will not agree to process any further data received by
     * {@link IFederationPluginEgress#fullSyncData(String, Optional)} and
     * {@link IFederationPluginEgress#steadyData(String, Collection)}. Also, after that time, the
     * {@link IFederationPluginEgress#cleanup()} will be invoked to allow the plugin to do cleaning logic. The
     * federation service will wait up to 5 seconds for the {@link CompletionStage} to be ready, before proceeding
     * forward.
     *
     * @return A {@link CompletionStage} indicating the abort() work has completed.
     */
    CompletionStage<Void> abort();
}
