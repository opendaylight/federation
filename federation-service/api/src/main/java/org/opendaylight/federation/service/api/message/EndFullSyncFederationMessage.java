/*
 * Copyright (c) 2017 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.api.message;

/**
 * Message indicating that the Full Sync stage has ended.
 */
public class EndFullSyncFederationMessage extends SequencedFederationMessage {

    public EndFullSyncFederationMessage(long sequenceId) {
        this.sequenceId = sequenceId;
    }

    @Override
    public String toString() {
        return "EndFullSyncFederationMessage [sequenceId=" + sequenceId + "]";
    }

}
