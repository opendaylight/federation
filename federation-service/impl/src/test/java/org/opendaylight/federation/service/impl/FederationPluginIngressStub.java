/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.opendaylight.federation.plugin.spi.IFederationPluginIngress;
import org.opendaylight.federation.service.common.api.EntityFederationMessage;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class FederationPluginIngressStub implements IFederationPluginIngress {

    private int beginCount = 0;
    private int endCount = 0;
    private int mismatchCount = 0;
    List<EntityFederationMessage<? extends DataObject>> consumedMsgs = new ArrayList<>();

    @Override
    public void beginFullSync() {
        beginCount++;
    }

    @Override
    public void endFullSync() {
        endCount++;
    }

    @Override
    public void consumeMsg(EntityFederationMessage<? extends DataObject> msg) {
        consumedMsgs.add(msg);
    }

    @Override
    public void resubscribe() {
        mismatchCount++;
    }

    @Override
    public String getPluginType() {
        return "TEST";
    }

    public int getBeginCount() {
        return beginCount;
    }

    public int getEndCount() {
        return endCount;
    }

    public int getMismatchCount() {
        return mismatchCount;
    }

    public List<EntityFederationMessage<? extends DataObject>> getConsumedMsgs() {
        return consumedMsgs;
    }

    public void reset() {
        beginCount = 0;
        endCount = 0;
        mismatchCount = 0;
        consumedMsgs.clear();
    }

    @Override
    public CompletableFuture<Void> abort() {
        return CompletableFuture.completedFuture(null);
    }
}
