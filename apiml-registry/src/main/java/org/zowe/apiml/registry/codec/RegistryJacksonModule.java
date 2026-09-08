/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.codec;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.io.IOException;

/**
 * Teaches a general-purpose {@link com.fasterxml.jackson.databind.ObjectMapper} how to handle
 * {@link ServiceInstance}.
 * <p>
 * The model is deliberately free of Jackson annotations - the wire format is the codec's business, not the
 * domain's. That leaves ordinary Jackson unable to see the model's accessors, since they are record-style
 * ({@code instanceId()}) rather than bean-style ({@code getInstanceId()}), so without this module a
 * {@code ServiceInstance} nested in some unrelated response body would serialise as {@code {}}.
 * <p>
 * Registering this makes any such body - the static-definition refresh result, for instance - come out in the
 * registry's own format, with the correct field order and with the ports present. The Eureka-based implementation
 * relied on {@code InstanceInfo} happening to be bean-shaped, and its ports were silently dropped as a result.
 */
public class RegistryJacksonModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    public RegistryJacksonModule() {
        super("apiml-registry");
        RegistryCodec codec = new RegistryCodec();
        addSerializer(ServiceInstance.class, new Serializer(codec));
        addDeserializer(ServiceInstance.class, new Deserializer(codec));
    }

    private static final class Serializer extends JsonSerializer<ServiceInstance> {

        private final RegistryCodec codec;

        private Serializer(RegistryCodec codec) {
            this.codec = codec;
        }

        @Override
        public void serialize(ServiceInstance value, JsonGenerator generator, SerializerProvider provider)
            throws IOException {

            generator.writeStartObject();
            codec.writeInstanceFields(generator, value);
            generator.writeEndObject();
        }
    }

    private static final class Deserializer extends JsonDeserializer<ServiceInstance> {

        private final RegistryCodec codec;

        private Deserializer(RegistryCodec codec) {
            this.codec = codec;
        }

        @Override
        public ServiceInstance deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonNode node = parser.readValueAsTree();
            return codec.decodeInstance(node.toString());
        }
    }

}
