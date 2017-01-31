/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.api;

import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.yangtools.yang.binding.DataObject;

// We need listenerPath + checkExistingDataPath because we can't read lists from MD-SAL
public class ListenerData {

    public String listenerId;
    public DataTreeIdentifier<? extends DataObject> listenerPath;
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
