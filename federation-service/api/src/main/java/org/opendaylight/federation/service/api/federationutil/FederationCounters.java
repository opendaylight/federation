/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.api.federationutil;

import org.opendaylight.infrautils.counters.api.OccurenceCounter;

public enum FederationCounters {

    sequence_mismatch,
    begin_fullsync,
    end_fullsync,
    consume_msg,
    msg_while_aborted,
    dynamic_queue_created,
    dynamic_queue_destroyed,
    remote_control_queue_created,
    subscription_message_sent,
    unsubscription_message_sent,
    dcn,
    steady_event_to_queue,
    steady_event_from_queue,
    msg_published,
    start_full_sync_msg_sent,
    end_full_sync_msg_sent,
    msg_sent_out_tx,
    fullsync_event;

    private OccurenceCounter counter;

    FederationCounters() {
        counter = new OccurenceCounter(getClass().getSimpleName(), name(), name());
    }

    public void inc() {
        counter.inc();
    }
}
