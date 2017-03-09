/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.impl;

import java.util.concurrent.atomic.AtomicBoolean;
import org.opendaylight.federation.plugin.spi.IFederationPluginIngress;
import org.opendaylight.federation.service.api.federationutil.FederationCounters;
import org.opendaylight.federation.service.api.federationutil.FederationUtils;
import org.opendaylight.federation.service.api.message.EndFullSyncFederationMessage;
import org.opendaylight.federation.service.api.message.FullSyncFailedFederationMessage;
import org.opendaylight.federation.service.api.message.SequencedFederationMessage;
import org.opendaylight.federation.service.api.message.StartFullSyncFederationMessage;
import org.opendaylight.federation.service.api.message.WrapperEntityFederationMessage;
import org.opendaylight.messagequeue.AbstractFederationMessage;
import org.opendaylight.messagequeue.IGeneralFederationConsumer;
import org.slf4j.Logger;

public class WrapperConsumer implements IGeneralFederationConsumer {

    private final IFederationPluginIngress pluginConsumer;
    private final Logger logger;
    private long nextExpectedSequence = 0;
    private long fullSyncBeginTime = 0;
    private final AtomicBoolean aborted = new AtomicBoolean(false);

    public WrapperConsumer(String remoteIp, IFederationPluginIngress pluginConsumer) {
        this.pluginConsumer = pluginConsumer;
        logger = FederationUtils.createLogger(remoteIp, WrapperConsumer.class);
    }

    @Override
    /* One thread polls messages from the message queue, so this method is never called concurrently.*/
    public void consumeMsg(AbstractFederationMessage msg) {
        if (aborted.get()) {
            FederationCounters.msg_while_aborted.inc();
            logger.debug("Received msg while in aborted state");
            return;
        }
        if (msg instanceof SequencedFederationMessage) {
            SequencedFederationMessage wrappedMsg = (SequencedFederationMessage) msg;
            logger.trace("consuming msg {}", wrappedMsg);
            FederationCounters.consume_msg.inc();
            if (inFirstMsgWhichIsNotStartFullSync(msg)) {
                logger.error("Expecting StartFullSyncFederationMessage as first message, but received {}", msg);
                if (aborted.compareAndSet(false, true)) {
                    pluginConsumer.resubscribe();
                }
                return;
            } else if (mismatchBetweenExpectedSequenceToActualSequence(wrappedMsg)) {
                if (aborted.compareAndSet(false, true)) {
                    pluginConsumer.resubscribe();
                    FederationCounters.sequence_mismatch.inc();
                    logger.error("Mismatch in sequence id was identified. expected {} got {}. Calling resubscribe",
                            nextExpectedSequence, wrappedMsg.getSequenceId());
                }
                return;
            } else {
                nextExpectedSequence = wrappedMsg.getSequenceId() + 1;
            }
        }
        if (msg instanceof StartFullSyncFederationMessage) {
            FederationCounters.begin_fullsync.inc();
            pluginConsumer.beginFullSync();
            fullSyncBeginTime = System.currentTimeMillis();
        } else if (msg instanceof EndFullSyncFederationMessage) {
            FederationCounters.end_fullsync.inc();
            pluginConsumer.endFullSync();
            EndFullSyncFederationMessage commitMsg = (EndFullSyncFederationMessage) msg;
            logger.info("Full Sync handled for {} ms with {} messages",
                    System.currentTimeMillis() - fullSyncBeginTime,
                    commitMsg.getSequenceId());
        } else if (msg instanceof FullSyncFailedFederationMessage) {
            FederationCounters.failed_fullsync.inc();
            pluginConsumer.fullSyncFailed();
            FullSyncFailedFederationMessage failedMsg = (FullSyncFailedFederationMessage) msg;
            logger.info("Full Sync failed after {} ms, and after {} messages",
                    System.currentTimeMillis() - fullSyncBeginTime,
                    failedMsg.getSequenceId());
        } else if (msg instanceof WrapperEntityFederationMessage) {
            WrapperEntityFederationMessage entityMessage = (WrapperEntityFederationMessage) msg;
            pluginConsumer.consumeMsg(entityMessage.getPayload());
        } else {
            logger.warn("Unknown message: " + msg);
        }
    }

    private boolean mismatchBetweenExpectedSequenceToActualSequence(SequencedFederationMessage wrappedMsg) {
        return wrappedMsg.getSequenceId() != nextExpectedSequence;
    }

    private boolean inFirstMsgWhichIsNotStartFullSync(AbstractFederationMessage msg) {
        return nextExpectedSequence == 0 && !(msg instanceof StartFullSyncFederationMessage);
    }
}
