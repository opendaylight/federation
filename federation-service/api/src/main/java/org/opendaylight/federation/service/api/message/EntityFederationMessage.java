/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.api.message;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class EntityFederationMessage<T extends DataObject, S extends DataObject> {

    private String dataStoreType;
    private String modificationType;
    private String metadata;
    private String originator;
    private String jsonInput;
    private Class<S> inputClassType;
    private transient T input;


    public EntityFederationMessage(String dataStoreType, String modificationType, String metadata, String originator,
            InstanceIdentifier<T> instanceIdentifier, T input, Class<S> inputClassType) {
        this.dataStoreType = Preconditions.checkNotNull(dataStoreType);
        this.modificationType = Preconditions.checkNotNull(modificationType);
        this.metadata = metadata;
        this.originator = originator;
        this.input = Preconditions.checkNotNull(input);
        this.jsonInput = BindingAwareJsonConverter
                .jsonStringFromDataObject(Preconditions.checkNotNull(instanceIdentifier), input);
        this.inputClassType = Preconditions.checkNotNull(inputClassType);
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

    public Class<S> getInputClassType() {
        return inputClassType;
    }

    public void setInputClassType(Class<S> inputClassType) {
        this.inputClassType = inputClassType;
    }

    public void setInput(InstanceIdentifier instanceIdentifier, T input) {
        this.jsonInput = BindingAwareJsonConverter.jsonStringFromDataObject(instanceIdentifier, input);
        this.input = input;
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
                + ", metadata=" + metadata + ", originator=" + originator + ", jsonInput=" + jsonInput
                + ", inputClassType=" + inputClassType + "]";
    }
}
