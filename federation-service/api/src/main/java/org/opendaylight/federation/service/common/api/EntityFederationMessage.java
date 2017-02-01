/*
 * Copyright (c) 2017 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.common.api;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.opendaylight.federation.plugin.spi.IFederationPluginEgress;
import org.opendaylight.federation.service.api.IFederationProducerMgr;
import org.opendaylight.federation.service.api.message.BindingAwareJsonConverter;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * This immutable message is created by the {@link IFederationPluginEgress} in order to send an entity to the consumer
 * site. The messages are expected to be created in the context of
 * {@link IFederationPluginEgress#fullSyncData(String, com.google.common.base.Optional)} and
 * {@link IFederationPluginEgress#steadyData(String, java.util.Collection)}, and sent by using the
 * {@link IFederationProducerMgr#publishMessage(EntityFederationMessage, String, String)}. An important thing to
 * remember is that in order to send a DTO, which is a class that extends {@link DataObject}, the federation
 * infrastructure converts it to JSON on the producer side. On the consumer side, the JSON will be converted back to a
 * {@link DataObject}. To convert back and forth, the entity must be part of a full YANG tree, and cannot stand alone by
 * itself. For example, in order to send an ElanInterface, the user must create a dummy ElanInterfaces and
 * add to it the single ElanInterface it wants to send.
 *
 * @param <T> The type of the entity that is sent in this message.
 */
public class EntityFederationMessage<T extends DataObject> {

    private String dataStoreType;
    private String modificationType;
    private String metadata;
    private String originator;
    private String jsonInput;
    private Class<? extends DataObject> inputClassType;
    private transient T input;

    public EntityFederationMessage(String dataStoreType, String modificationType, String metadata, String originator,
        InstanceIdentifier<T> instanceIdentifier, T input) {
        this.dataStoreType = Preconditions.checkNotNull(dataStoreType);
        this.modificationType = Preconditions.checkNotNull(modificationType);
        this.metadata = metadata;
        this.originator = originator;
        this.input = Preconditions.checkNotNull(input);
        this.jsonInput =
            BindingAwareJsonConverter.jsonStringFromDataObject(Preconditions.checkNotNull(instanceIdentifier), input);
        this.inputClassType = input.getClass();
    }

    @VisibleForTesting
    public EntityFederationMessage() {
    }

    public String getDataStoreType() {
        return dataStoreType;
    }

    public String getModificationType() {
        return modificationType;
    }

    public String getMetadata() {
        return metadata;
    }

    public String getOriginator() {
        return originator;
    }

    public String getJsonInput() {
        return jsonInput;
    }

    public Class<? extends DataObject> getInputClassType() {
        return inputClassType;
    }

    @SuppressWarnings("unchecked")
    public synchronized T getInput() {
        if (input != null) {
            return input;
        }

        if (Strings.isNullOrEmpty(jsonInput)) {
            return null;
        }

        NormalizedNode<?, ?> normalizedNode = BindingAwareJsonConverter.normalizedNodeFromJsonString(jsonInput);
        input = (T) BindingAwareJsonConverter.dataObjectFromNormalizedNode(BindingReflections.findQName(inputClassType),
            normalizedNode);
        return input;
    }

    @Override
    public String toString() {
        return "EntityFederationMessage [dataStoreType=" + dataStoreType + ", modificationType=" + modificationType
            + ", metadata=" + metadata + ", originator=" + originator + ", jsonInput=" + jsonInput + ", inputClassType="
            + inputClassType + "]";
    }
}
