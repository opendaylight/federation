/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.api;

import java.util.concurrent.CompletionStage;
import org.opendaylight.federation.service.api.message.EntityFederationMessage;
import org.opendaylight.yangtools.yang.binding.DataObject;

public interface IFederationPluginIngress {

    void beginFullSync();

    void endFullSync();

    void consumeMsg(EntityFederationMessage<? extends DataObject, ? extends DataObject> msg);

    /** Will cause recreation of the ingress plugin and reregistration.
     * Cleaner design would hide it from the ingress plugin, and keep all the logic inside the federation service.
     */
    void resubscribe();

    String getPluginType();

    CompletionStage<Void> abort();
}
