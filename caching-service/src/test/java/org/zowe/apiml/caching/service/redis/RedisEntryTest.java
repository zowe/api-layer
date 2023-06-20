/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.service.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.redis.exceptions.RedisEntryException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RedisEntryTest {
    private static final String SERVICE_ID = "my-service";
    private static final KeyValue KEY_VALUE = new KeyValue("key", "value");
    private static final String KEY_VALUE_SERIALIZED = "{\"key\":\"key\",\"value\":\"value\",\"created\":\"" + KEY_VALUE.getCreated() + "\"}";

    @Nested
    class whenInstantiating {
        @Test
        void givenKeyValue_thenDirectlyUseKeyValue() {
            RedisEntry actual = new RedisEntry(SERVICE_ID, KEY_VALUE);
            assertThat(actual.getServiceId(), is(SERVICE_ID));
            assertThat(actual.getEntry(), is(KEY_VALUE));
        }

        @Test
        void givenSerializedKeyValue_thenDeserializeKeyValue() throws RedisEntryException {
            RedisEntry actual = new RedisEntry(SERVICE_ID, KEY_VALUE_SERIALIZED);
            assertThat(actual.getServiceId(), is(SERVICE_ID));
            assertThat(actual.getEntry(), is(KEY_VALUE));
        }

        @Test
        void givenInvalidSerializedKeyValue_thenThrowException() {
            RedisEntryException thrown = assertThrows(RedisEntryException.class, () -> new RedisEntry(SERVICE_ID, "bad serialized"));
            assertThat(thrown.getCause(), instanceOf(JsonProcessingException.class));
            assertThat(thrown.getMessage(), is("Failure deserializing the entry to a KeyValue object"));
        }

        @Test
        void givenNullSerializedKeyValue_thenThrowException() {
            RedisEntryException thrown = assertThrows(RedisEntryException.class, () -> new RedisEntry(SERVICE_ID, (String) null));
            assertThat(thrown.getMessage(), is("Failure deserializing the entry to a KeyValue object"));
        }
    }

    @Nested
    class whenGettingString {
        @Test
        void givenSerializableKeyValue_thenReturnStringJson() throws RedisEntryException {
            RedisEntry underTest = new RedisEntry(SERVICE_ID, KEY_VALUE);
            String result = underTest.getEntryAsString();
            assertThat(result, is(KEY_VALUE_SERIALIZED));
        }

        @Test
        void givenSerializingError_thenThrowRedisEntryException() throws JsonProcessingException {
            ObjectMapper mapper = mock(ObjectMapper.class);
            when(mapper.writeValueAsString(any())).thenThrow(new RuntimeException("error"));

            RedisEntry underTest = new RedisEntry(SERVICE_ID, KEY_VALUE, mapper);
            RedisEntryException ex = assertThrows(RedisEntryException.class, underTest::getEntryAsString);
            assertThat(ex.getMessage(), is("Failure serializing the entry as a String"));
        }
    }
}
