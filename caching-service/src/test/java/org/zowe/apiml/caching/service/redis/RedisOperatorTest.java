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

import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zowe.apiml.caching.model.KeyValue;

import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RedisOperatorTest {
    private RedisOperator underTest;
    private static final String SERVICE_ID = "serviceId";
    private static final String KEY = "key";
    private static final String VALUE = "value";
    private static final KeyValue KEY_VALUE = new KeyValue(KEY, VALUE);
    private static final RedisEntry REDIS_ENTRY = new RedisEntry(SERVICE_ID, KEY_VALUE);

    @Mock
    private RedisAsyncCommands<String, String> redisCommands;

    @BeforeEach
    void setUp() {
        underTest = new RedisOperator(redisCommands);
    }

    @Nested
    class whenCreating {

        @Mock
        private RedisFuture<Boolean> future;

        @BeforeEach
        void mockRedisCommand() {
            when(redisCommands.hsetnx(any(), any(), any())).thenReturn(future);
        }

        @Test
        void givenNewEntry_thenReturnTrue() throws ExecutionException, InterruptedException {
            when(future.get()).thenReturn(true);

            boolean result = underTest.create(REDIS_ENTRY);
            assertTrue(result);
        }

        @Test
        void givenExistingEntry_thenReturnFalse() throws ExecutionException, InterruptedException {
            when(future.get()).thenReturn(false);

            boolean result = underTest.create(REDIS_ENTRY);
            assertFalse(result);
        }

        @Test
        void givenInterruptedException_thenThrowRetryException() throws ExecutionException, InterruptedException {
            when(future.get()).thenThrow(new InterruptedException());

            assertThrows(RetryableRedisException.class, () -> underTest.create(REDIS_ENTRY));
        }

        @Test
        void givenExecutionException_thenThrowRetryException() throws ExecutionException, InterruptedException {
            when(future.get()).thenThrow(new ExecutionException(new Exception()));

            assertThrows(RetryableRedisException.class, () -> underTest.create(REDIS_ENTRY));
        }
    }

    @Nested
    class whenUpdating {

    }

    @Nested
    class whenGettingOneEntry {

        @Mock
        private RedisFuture<String> future;

        @BeforeEach
        void mockRedisCommand() {
            when(redisCommands.hget(any(), any())).thenReturn(future);
        }

        @Test
        void givenExistingKey_thenReturnEntry() throws ExecutionException, InterruptedException {
            String expectedSerialization = "{\"key\":\"key\",\"value\":\"value\",\"created\":\"" + KEY_VALUE.getCreated() + "\"}";
            when(future.get()).thenReturn(expectedSerialization);

            RedisEntry result = underTest.get(SERVICE_ID, KEY);
            assertThat(result.getServiceId(), is(SERVICE_ID));
            assertThat(result.getEntry().getKey(), is(KEY));
            assertThat(result.getEntry().getValue(), is(VALUE));
        }

        @Test
        void givenNotExistingKey_thenReturnNull() throws ExecutionException, InterruptedException {
            when(future.get()).thenReturn(null);

            RedisEntry result = underTest.get(SERVICE_ID,"bad key");
            assertThat(result, isNull());
        }

        @Test
        void givenKeyWithInvalidSerializedValue_thenReturnNull() throws ExecutionException, InterruptedException {
            String badSerialization = "bad key value";
            when(future.get()).thenReturn(badSerialization);

            RedisEntry result = underTest.get(SERVICE_ID,"KEY");
            assertThat(result, isNull());
        }

        @Test
        void givenInterruptedException_thenThrowRetryException() throws ExecutionException, InterruptedException {
            when(future.get()).thenThrow(new InterruptedException());

            assertThrows(RetryableRedisException.class, () -> underTest.get(SERVICE_ID, KEY));
        }

        @Test
        void givenExecutionException_thenThrowRetryException() throws ExecutionException, InterruptedException {
            when(future.get()).thenThrow(new ExecutionException(new Exception()));

            assertThrows(RetryableRedisException.class, () -> underTest.get(SERVICE_ID, KEY));
        }
    }

    @Nested
    class whenGettingAllEntries {

        @Mock
        private RedisFuture<String> future;

        @BeforeEach
        void mockRedisCommand() {
            when(redisCommands.hget(any(), any())).thenReturn(future);
        }


    }

    @Nested
    class whenDeleting {

    }
}
