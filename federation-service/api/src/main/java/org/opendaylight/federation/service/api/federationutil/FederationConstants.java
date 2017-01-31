/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.api.federationutil;

public interface FederationConstants {
    int MSG_NOT_IN_TX_ID = -1;
    String LOGGER_PREFIX = "federation.";
    String CONTROL_QUEUE = "FederationControlQueue";
    String DYNAMIC_QUEUE_SUFFIX = "_FederationDynamicQueue";
    String CLUSTERING_SERVICE_ID = "FEDERATION_GROUP";
}
