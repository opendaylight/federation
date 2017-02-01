/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.federation.service.api.message.EndFullSyncFederationMessage;
import org.opendaylight.federation.service.api.message.StartFullSyncFederationMessage;
import org.opendaylight.federation.service.api.message.WrapperEntityFederationMessage;
import org.opendaylight.federation.service.common.api.EntityFederationMessage;

@RunWith(MockitoJUnitRunner.class)
public class WrapperConsumerTest {

    private WrapperConsumer testedClass;

    private FederationPluginIngressStub pluginIngressStub;

    @Before
    public void setUp() throws Exception {
        pluginIngressStub = new FederationPluginIngressStub();
        testedClass = new WrapperConsumer("1.1.1.1", pluginIngressStub);
    }

    @Test
    public void simpleFlow__singleMsg() {
        testedClass.consumeMsg(startFullSyncMsg());
        testedClass.consumeMsg(buildMsg(1));
        testedClass.consumeMsg(endFullSyncMsg(2));

        assertEquals(1, pluginIngressStub.getBeginCount());
        assertEquals(1, pluginIngressStub.getConsumedMsgs().size());
        assertEquals(1, pluginIngressStub.getEndCount());
    }

    @Test
    public void simpleFlow__multipleMsgs() {
        testedClass.consumeMsg(startFullSyncMsg());
        testedClass.consumeMsg(buildMsg(1));
        testedClass.consumeMsg(buildMsg(2));
        testedClass.consumeMsg(endFullSyncMsg(3));

        assertEquals(1, pluginIngressStub.getBeginCount());
        assertEquals(2, pluginIngressStub.getConsumedMsgs().size());
        assertEquals(1, pluginIngressStub.getEndCount());
    }

    @Test
    public void mismatch__Simple() {
        testedClass.consumeMsg(startFullSyncMsg());
        WrapperEntityFederationMessage msg = buildMsg(1);
        testedClass.consumeMsg(msg);
        EndFullSyncFederationMessage cfm =
                new EndFullSyncFederationMessage(2);
        testedClass.consumeMsg(cfm);

        assertEquals(1, pluginIngressStub.getBeginCount());
        assertEquals(1, pluginIngressStub.getEndCount());
        assertEquals(1, pluginIngressStub.getConsumedMsgs().size());

        pluginIngressStub.reset();
        msg = buildMsg(3);
        testedClass.consumeMsg(msg);
        assertEquals(1, pluginIngressStub.getConsumedMsgs().size());

        pluginIngressStub.reset();
        msg = buildMsg(5);
        testedClass.consumeMsg(msg);
        assertEquals(0, pluginIngressStub.getConsumedMsgs().size());
        assertEquals(1, pluginIngressStub.getMismatchCount());
    }

    private StartFullSyncFederationMessage startFullSyncMsg() {
        return new StartFullSyncFederationMessage();
    }

    private EndFullSyncFederationMessage endFullSyncMsg(int sequence) {
        return new EndFullSyncFederationMessage(sequence);
    }

    @SuppressWarnings("rawtypes")
    private WrapperEntityFederationMessage buildMsg(long sequenceId) {
        return (WrapperEntityFederationMessage) new WrapperEntityFederationMessage(new EntityFederationMessage())
                .setSequenceId(sequenceId);
    }
}
