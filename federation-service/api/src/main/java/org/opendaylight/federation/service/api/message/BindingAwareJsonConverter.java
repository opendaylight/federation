/*
 * Copyright (c) 2017 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.federation.service.api.message;

import com.google.common.collect.FluentIterable;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import javassist.ClassPool;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yangtools.sal.binding.generator.util.JavassistUtils;
import org.opendaylight.yangtools.yang.binding.BindingStreamEventWriter;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonWriterFactory;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * This util is used by the federation infrastructure in order to convert DTOs to JSON and JSON to DTOs. It is expected
 * to be pre-initialized with all the entity types that plugins might send through it by invoking
 * {@link BindingAwareJsonConverter#init(Iterable)}.
 */
public class BindingAwareJsonConverter {

    private static SchemaContext context;
    private static BindingRuntimeContext bindingContext;
    private static BindingNormalizedNodeCodecRegistry codecRegistry;

    /**
     * Before DTOs can be converted to JSON and back, this util must be initialized with all the possible entities that
     * will be federated. The Iterable can be populated for example like this:
     * BindingReflections.getModuleInfo(Topology.class)
     *
     * @param moduleInfos The collection of all entity types {@link YangModuleInfo} that can be federated.
     */
    public static void init(Iterable<? extends YangModuleInfo> moduleInfos) {
        final ModuleInfoBackedContext moduleContext = ModuleInfoBackedContext.create();
        moduleContext.addModuleInfos(moduleInfos);
        context = moduleContext.tryToCreateSchemaContext().get();
        bindingContext = BindingRuntimeContext.create(moduleContext, context);

        final BindingNormalizedNodeCodecRegistry bindingStreamCodecs = new BindingNormalizedNodeCodecRegistry(
            StreamWriterGenerator.create(JavassistUtils.forClassPool(ClassPool.getDefault())));
        bindingStreamCodecs.onBindingRuntimeContextUpdated(bindingContext);
        codecRegistry = bindingStreamCodecs;
    }

    /**
     * Converts a {@link DataObject} to a JSON representation in a string using the relevant YANG schema if it is
     * present. This defaults to using a {@link org.opendaylight.yangtools.yang.model.api.SchemaContextListener} if
     * running an OSGi environment or {@link BindingReflections#loadModuleInfos()} if run while not in an OSGi
     * environment or if the schema isn't available via
     * {@link org.opendaylight.yangtools.yang.model.api.SchemaContextListener}.
     *
     * @param path {@literal InstanceIdentifier<?>}
     * @param object DataObject
     * @return String
     */
    public static String jsonStringFromDataObject(InstanceIdentifier<?> path, DataObject object) {
        return jsonStringFromDataObject(path, object, false);
    }

    /**
     * Converts a {@link DataObject} to a JSON representation in a string using the relevant YANG schema if it is
     * present. This defaults to using a {@link org.opendaylight.yangtools.yang.model.api.SchemaContextListener} if
     * running an OSGi environment or {@link BindingReflections#loadModuleInfos()} if run while not in an OSGi
     * environment or if the schema isn't available via
     * {@link org.opendaylight.yangtools.yang.model.api.SchemaContextListener}.
     *
     * @param path {@literal InstanceIdentifier<?>}
     * @param object DataObject
     * @param pretty boolean
     * @return String
     */
    public static String jsonStringFromDataObject(InstanceIdentifier<?> path, DataObject object, boolean pretty) {
        if (object == null) {
            return null;
        }

        final SchemaPath scPath = SchemaPath.create(FluentIterable.from(path.getPathArguments())
            .transform(input -> BindingReflections.findQName(input.getType())), true);

        final Writer writer = new StringWriter();
        final NormalizedNodeStreamWriter domWriter;
        if (pretty) {
            domWriter = JSONNormalizedNodeStreamWriter.createExclusiveWriter(JSONCodecFactory.create(context),
                scPath.getParent(), scPath.getLastComponent().getNamespace(),
                JsonWriterFactory.createJsonWriter(writer, 2));
        } else {
            domWriter = JSONNormalizedNodeStreamWriter.createExclusiveWriter(JSONCodecFactory.create(context),
                scPath.getParent(), scPath.getLastComponent().getNamespace(),
                JsonWriterFactory.createJsonWriter(writer));
        }
        final BindingStreamEventWriter bindingWriter = codecRegistry.newWriter(path, domWriter);

        try {
            codecRegistry.getSerializer(path.getTargetType()).serialize(object, bindingWriter);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return writer.toString();
    }

    public static NormalizedNode<?, ?> normalizedNodeFromJsonString(final String inputJson) {
        final NormalizedNodeResult result = new NormalizedNodeResult();
        final NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
        final JsonParserStream jsonParser = JsonParserStream.create(streamWriter, context);
        jsonParser.parse(new JsonReader(new StringReader(inputJson)));
        final NormalizedNode<?, ?> transformedInput = result.getResult();
        return transformedInput;
    }

    public static DataObject dataObjectFromNormalizedNode(QName qname, NormalizedNode<?, ?> nn) {
        return codecRegistry.fromNormalizedNode(YangInstanceIdentifier.of(qname), nn).getValue();
    }
}
