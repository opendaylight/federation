/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.api.federationutil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FederationUtils {

    public static Logger createLogger(String remoteIp, Class<?> clazz) {
        return LoggerFactory.getLogger(FederationConstants.LOGGER_PREFIX + remoteIp + "." + clazz.getSimpleName());
    }

    public static String createDynamicQueueName(String remoteIp, String localIp) {
        return remoteIp + "_" + localIp + "_" + System.currentTimeMillis() + FederationConstants.DYNAMIC_QUEUE_SUFFIX;
    }

    public static String createControlQueueName(String localIp) {
        return localIp + "_" + FederationConstants.CONTROL_QUEUE;
    }
}
