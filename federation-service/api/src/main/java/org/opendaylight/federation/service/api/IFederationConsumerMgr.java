/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.api;

public interface IFederationConsumerMgr {
    void subscribe(String remoteIp, Object payload, IFederationPluginIngress pluginConsumer);

    void subscribe(String remoteIp, Object payload, IFederationPluginIngress pluginConsumer,
            boolean requestMutualSubscription);

    void unsubscribe(String remoteIp);

}
