/*
 * Copyright (c) 2017 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.api.message;

import com.google.common.base.Preconditions;
import org.opendaylight.messagequeue.AbstractFederationMessage;

/**
 * Unsubscription issued by the consumer site to the producer site in order to close and clean the federation session.
 */
public class UnsubscribeMessage extends AbstractFederationMessage {

    private final String dynamicQueueName;
    private final String pluginType;
    private final String contextId;
    private final Object payload;

    public UnsubscribeMessage(String dynamicQueueName, String pluginType, String contextId, Object payload) {
        super();
        this.dynamicQueueName = Preconditions.checkNotNull(dynamicQueueName);
        this.pluginType = Preconditions.checkNotNull(pluginType);
        this.payload = payload;
        this.contextId = Preconditions.checkNotNull(contextId);
    }

    public String getDynamicQueueName() {
        return dynamicQueueName;
    }

    public String getPluginType() {
        return pluginType;
    }

    public Object getPayload() {
        return payload;
    }

    public String getContextId() {
        return contextId;
    }

    @Override
    public String toString() {
        return "UnsubscribeMessage [dynamicQueueName=" + dynamicQueueName + ", pluginType=" + pluginType + ", payload="
                + payload + ", contextId=" + contextId + "]";
    }
}
