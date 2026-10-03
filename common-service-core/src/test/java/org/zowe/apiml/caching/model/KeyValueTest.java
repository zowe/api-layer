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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code KeyValue} is the value type of the replicated and persisted {@code zoweCache}, which among other
 * things holds the salt that all personal access token hashing depends on. Adding a field to it is therefore
 * an upgrade-compatibility question, not just a Jackson one - so the shape of both serialized forms is
 * asserted rather than assumed.
 */
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

        /**
         * The whole reason the field is {@code transient}: a previous-release node has to be able to read
         * what this one writes, and the other way round, for the lifetime of a rolling restart.
         */
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

        /**
         * Jackson does not honour the transient marker by default, so the property still crosses the wire -
         * which is what carries the lifespan from ZAAS to the caching service.
         */
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

    /**
     * {@code InfinispanStorage.update} logs a whole {@code KeyValue}, so a field appearing in toString would
     * silently change that log line on every generic cache write, personal access token or not.
     */
    @Test
    void thenTheTtlIsNotPrinted() {
        assertFalse(new KeyValue("key", "value", 60L).toString().contains("ttlSeconds"));
    }

    /**
     * Lombok excludes transient fields from equals and hashCode by construction; the round-trip copy is the
     * only way to get two instances whose 'created' timestamps agree.
     */
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
