/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.api.message;

import org.opendaylight.messagequeue.AbstractFederationMessage;

public abstract class SequencedFederationMessage extends AbstractFederationMessage {
    protected long sequenceId;

    public long getSequenceId() {
        return sequenceId;
    }

    public SequencedFederationMessage setSequenceId(long sequenceId) {
        this.sequenceId = sequenceId;
        return this;
    }
}
