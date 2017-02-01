/*
 * Copyright (c) 2017 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.common.api;

import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.federation.plugin.spi.IFederationPluginEgress;
import org.opendaylight.federation.service.api.IFederationProducerMgr;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * The description of an entity which the {@link IFederationPluginEgress} is interested in getting notifications by it.
 * The {@link IFederationPluginEgress} will eventually supply a List of {@link ListenerData} as a response to
 * {@link IFederationPluginEgress#getListenersData()} to declare all entities the plugin is interested getting
 * notifications on.
 */
public class ListenerData {

    /**
     * A unique identifier for the specific entity type. This identifier will later be used by the
     * {@link IFederationProducerMgr} when it will invoke
     * {@link IFederationPluginEgress#fullSyncData(String, com.google.common.base.Optional)} and
     * {@link IFederationPluginEgress#steadyData(String, java.util.Collection)}.
     */
    public String listenerId;

    /**
     * The {@link DataTreeIdentifier} that the {@link IFederationProducerMgr} is expected to LISTEN on for
     * {@link IFederationPluginEgress#steadyData(String, java.util.Collection)} purposes. This
     * {@link DataTreeIdentifier} will be used for invoking
     * {@link org.opendaylight.controller.md.sal.binding.api.DataBroker} registerDataTreeChangeListener().
     */
    public DataTreeIdentifier<? extends DataObject> listenerPath;

    /**
     * The {@link DataTreeIdentifier} that the {@link IFederationProducerMgr} is expected to READ for
     * {@link IFederationPluginEgress#fullSyncData(String, com.google.common.base.Optional)} purposes. This
     * {@link DataTreeIdentifier} will be used for invoking
     * {@link org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction} read(). The reason this path is
     * different than the {@link ListenerData#listenerPath} is because the listen path is used for registering a
     * listener, and this can use a YANG wildcard. Users can register a listener on a LIST Node, but cannot read a LIST
     * Node. So, for checking existing data, the plugin can supply a {@link DataTreeIdentifier} of the CONTAINER node
     * which contains the LIST node, and extract the LIST by itself when the
     * {@link IFederationPluginEgress#fullSyncData(String, com.google.common.base.Optional)} is called.
     */
    public DataTreeIdentifier<? extends DataObject> checkExistingDataPath;

    public ListenerData(String listenerKey, DataTreeIdentifier<? extends DataObject> listenerPath,
        DataTreeIdentifier<? extends DataObject> checkExistingDataPath) {
        super();
        this.listenerId = listenerKey;
        this.listenerPath = listenerPath;
        this.checkExistingDataPath = checkExistingDataPath;
    }

    @Override
    public String toString() {
        return "ListenerData [listenerId=" + listenerId + ", listenerPath=" + listenerPath + ", checkExistingDataPath="
            + checkExistingDataPath + "]";
    }
}
