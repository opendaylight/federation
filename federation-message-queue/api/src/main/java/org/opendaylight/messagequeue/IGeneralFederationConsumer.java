/*
 * Copyright (c) 2017 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.messagequeue;

/**
 * Consumer of messages from the message queue. This is created internally by the federation service infrastructure.
 */
public interface IGeneralFederationConsumer {
    void consumeMsg(AbstractFederationMessage msg);
}
