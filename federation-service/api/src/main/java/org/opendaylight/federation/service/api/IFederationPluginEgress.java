/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.api;

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionStage;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yangtools.yang.binding.DataObject;

public interface IFederationPluginEgress {

    void steadyData(String listenerKey, Collection<DataTreeModification<? extends DataObject>> data);

    void fullSyncData(String listenerKey, Optional<? extends DataObject> existingData);

    List<ListenerData> getListenersData();

    void cleanup();

    CompletionStage<Void> abort();

}
