/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.api.message;

import com.google.common.base.Preconditions;
import org.opendaylight.messagequeue.AbstractFederationMessage;

public class SubscribeMessage extends AbstractFederationMessage {
    private final String dynamicQueueName;
    private final String pluginType;
    private final Object payload;
    private final String subscriberIp;
    private final String contextId;
    private final boolean requestMutualSubscription;

    public SubscribeMessage(String dynamicQueueName, String pluginType, Object payload, String subscriberIp,
            String contextId) {
        this(dynamicQueueName, pluginType, payload, subscriberIp, contextId, false);
    }

    public SubscribeMessage(String dynamicQueueName, String pluginType, Object payload, String subscriberIp,
            String contextId, boolean requestMutualSubscription) {
        super();
        this.dynamicQueueName = Preconditions.checkNotNull(dynamicQueueName);
        this.pluginType = Preconditions.checkNotNull(pluginType);
        this.payload = payload;
        this.requestMutualSubscription = requestMutualSubscription;
        this.subscriberIp = Preconditions.checkNotNull(subscriberIp);
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

    @Override
    public String toString() {
        return "SubscribeMessage [dynamicQueueName=" + dynamicQueueName + ", pluginType=" + pluginType + ", payload="
                + payload + ", subscriberIp=" + subscriberIp + ", contextId=" + contextId
                + ", requestMutualSubscription=" + requestMutualSubscription + "]";
    }

    public String getSubscriberIp() {
        return subscriberIp;
    }

    public boolean isRequestMutualSubscription() {
        return requestMutualSubscription;
    }

    public String getContextId() {
        return contextId;
    }

}
