/*
 * Copyright (c) 2017 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.api.message;

import org.opendaylight.federation.service.common.api.EntityFederationMessage;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * A wrapper for the {@link EntityFederationMessage} used by the plugins. This wrapper is not exposed to the plugins and
 * may be used by the infrastructure to add metadata about the message.
 */
public class WrapperEntityFederationMessage extends SequencedFederationMessage {

    private final EntityFederationMessage<? extends DataObject> payload;

    public WrapperEntityFederationMessage(EntityFederationMessage<? extends DataObject> payload) {
        this.payload = payload;
    }

    public EntityFederationMessage<? extends DataObject> getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "WrapperEntityFederationMessage [payload=" + payload + "]";
    }
}
