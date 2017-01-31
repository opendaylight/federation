/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.api.message;

public class WrapperEntityFederationMessage extends SequencedFederationMessage {

    private EntityFederationMessage payload;

    public EntityFederationMessage getPayload() {
        return payload;
    }

    public WrapperEntityFederationMessage setPayload(EntityFederationMessage payload) {
        this.payload = payload;
        return this;
    }

    @Override
    public String toString() {
        return "WrapperEntityFederationMessage [payload=" + payload + "]";
    }
}
