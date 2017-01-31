/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.api;

import org.opendaylight.federation.service.api.message.EntityFederationMessage;
import org.opendaylight.yangtools.yang.binding.DataObject;

public interface IFederationProducerMgr {
    void publishMessage(EntityFederationMessage<? extends DataObject, ? extends DataObject> msg, String queueName,
            String contextId);

    void attachPluginFactory(String pluginType, IPluginFactory factory);

    void detachPluginFactory(String pluginType);
}
