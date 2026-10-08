/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class KeyValueTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private byte[] javaSerialize(KeyValue keyValue) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var out = new ObjectOutputStream(bytes)) {
            out.writeObject(keyValue);
        }
        return bytes.toByteArray();
    }

    private KeyValue javaDeserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (var in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (KeyValue) in.readObject();
        }
    }

    @Nested
    class WhenJavaSerialized {

        @Test
        void thenTheTtlIsNotPartOfTheStream() throws Exception {
            KeyValue withoutTtl = new KeyValue("key", "value");
            KeyValue withTtl = new KeyValue("key", "value");
            withTtl.setServiceId(withoutTtl.getServiceId());
            withTtl.setTtlSeconds(1234L);

            byte[] plain = javaSerialize(withoutTtl);
            byte[] withTtlBytes = javaSerialize(withTtl);

            assertArrayEquals(plain, withTtlBytes, "the Java-serialized form must not change shape");
            assertFalse(new String(plain, StandardCharsets.ISO_8859_1).contains("ttlSeconds"));
        }

        @Test
        void thenTheStreamStillCarriesTheFieldsOfThePreviousShape() throws Exception {
            String stream = new String(javaSerialize(new KeyValue("key", "value")), StandardCharsets.ISO_8859_1);

            assertTrue(stream.contains("key"));
            assertTrue(stream.contains("value"));
            assertTrue(stream.contains("serviceId"));
            assertTrue(stream.contains("created"));
        }

        @Test
        void thenItRoundTripsWithoutTheTtl() throws Exception {
            KeyValue original = new KeyValue("key", "value", 60L);
            original.setServiceId("service");

            KeyValue restored = javaDeserialize(javaSerialize(original));

            assertEquals("key", restored.getKey());
            assertEquals("value", restored.getValue());
            assertEquals("service", restored.getServiceId());
            assertNull(restored.getTtlSeconds(), "a transient field does not survive Java serialization by design");
        }
    }

    @Nested
    class WhenJsonSerialized {

        @Test
        void givenATtl_thenItIsEmitted() throws Exception {
            String json = mapper.writeValueAsString(new KeyValue("key", "value", 60L));

            assertTrue(json.contains("\"ttlSeconds\":60"), json);
        }

        @Test
        void givenNoTtl_thenTheWireFormatIsUnchanged() throws Exception {
            String json = mapper.writeValueAsString(new KeyValue("key", "value"));

            assertFalse(json.contains("ttlSeconds"), json);
        }

        @Test
        void thenItRoundTrips() throws Exception {
            KeyValue restored = mapper.readValue(mapper.writeValueAsString(new KeyValue("key", "value", 60L)), KeyValue.class);

            assertEquals(60L, restored.getTtlSeconds());
        }

        @Test
        void givenAPayloadWithoutTheTtl_thenItStillParses() throws Exception {
            KeyValue restored = mapper.readValue("{\"key\":\"k\",\"value\":\"v\",\"created\":\"1\"}", KeyValue.class);

            assertEquals("k", restored.getKey());
            assertNull(restored.getTtlSeconds());
        }
    }

    @Nested
    class WhenJsonDeserializedWithJackson3 {

        private final JsonMapper jsonMapper = JsonMapper.builder().build();

        @Test
        void givenAllFields_thenTheFinalFieldsAreBound() {
            KeyValue restored = jsonMapper.readValue(
                "{\"key\":\"k\",\"value\":\"v\",\"serviceId\":\"service\",\"created\":\"1\",\"ttlSeconds\":60}", KeyValue.class);

            assertEquals("k", restored.getKey());
            assertEquals("v", restored.getValue());
            assertEquals("service", restored.getServiceId());
            assertEquals("1", restored.getCreated());
            assertEquals(60L, restored.getTtlSeconds());
        }

        @Test
        void givenNoCreated_thenTheCurrentTimeIsUsed() {
            long before = System.currentTimeMillis();

            KeyValue restored = jsonMapper.readValue("{\"key\":\"k\",\"value\":\"v\"}", KeyValue.class);

            assertTrue(Long.parseLong(restored.getCreated()) >= before);
            assertEquals("", restored.getServiceId());
        }

        @Test
        void givenNoKeyAndNoValue_thenTheyAreNull() {
            KeyValue restored = jsonMapper.readValue("{}", KeyValue.class);

            assertNull(restored.getKey());
            assertNull(restored.getValue());
        }

        @Test
        void thenItRoundTrips() {
            KeyValue original = new KeyValue("key", "value", 60L);
            original.setServiceId("service");

            KeyValue restored = jsonMapper.readValue(jsonMapper.writeValueAsString(original), KeyValue.class);

            assertEquals(original, restored);
            assertEquals(60L, restored.getTtlSeconds());
        }
    }

    @Test
    void thenTheTtlIsNotPrinted() {
        assertFalse(new KeyValue("key", "value", 60L).toString().contains("ttlSeconds"));
    }

    @Test
    void thenTheTtlDoesNotAffectEquality() throws Exception {
        KeyValue original = new KeyValue("key", "value");
        KeyValue copy = javaDeserialize(javaSerialize(original));
        assertEquals(original, copy);

        copy.setTtlSeconds(99L);

        assertEquals(original, copy);
        assertEquals(original.hashCode(), copy.hashCode());
    }
}
