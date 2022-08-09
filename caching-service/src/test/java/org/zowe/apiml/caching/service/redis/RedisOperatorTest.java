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

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.redis.exceptions.RedisEntryException;
import org.zowe.apiml.caching.service.redis.exceptions.RedisOutOfMemoryException;
import org.zowe.apiml.caching.service.redis.exceptions.RetryableRedisException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisOperatorTest {
    private RedisOperator underTest;
    private static final String SERVICE_ID = "serviceId";
    private static final String KEY = "key";
    private static final String VALUE = "value";
    private static final KeyValue KEY_VALUE = new KeyValue(KEY, VALUE);
    private static final RedisEntry REDIS_ENTRY = new RedisEntry(SERVICE_ID, KEY_VALUE);
    private static final String VALID_SERIALIZED_ENTRY = "{\"key\":\"key\",\"value\":\"value\",\"created\":\"" + KEY_VALUE.getCreated() + "\"}";

    @Mock
    private RedisAsyncCommands<String, String> redisCommands;

    @Mock
    private StatefulRedisMasterReplicaConnection<String, String> redisConnection;

    private RedisClient redisClient;

    @BeforeEach
    void setUp() {
        redisClient = mock(RedisClient.class);
        underTest = new RedisOperator(redisClient, redisConnection, redisCommands);
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
        void givenNewEntry_thenReturnTrue() throws ExecutionException, InterruptedException, RedisOutOfMemoryException {
            when(future.get()).thenReturn(true);

            boolean result = underTest.create(REDIS_ENTRY);
            assertTrue(result);
        }

        @Test
        void givenExistingEntry_thenReturnFalse() throws ExecutionException, InterruptedException, RedisOutOfMemoryException {
            when(future.get()).thenReturn(false);

            boolean result = underTest.create(REDIS_ENTRY);
            assertFalse(result);
        }

        @Test
        void givenRedisOutOfMemory_thenThrowOutOfMemoryException() throws ExecutionException, InterruptedException {
            when(future.get()).thenThrow(new ExecutionException(new RedisCommandExecutionException("maxmemory")));

            assertThrows(RedisOutOfMemoryException.class, () -> underTest.create(REDIS_ENTRY));
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

    // Outside whenCreating because test fails with unnecessary stubbings error
    @Test
    void givenRedisEntryException_thenReturnFalse() throws RedisEntryException, RedisOutOfMemoryException {
        RedisEntry entry = mock(RedisEntry.class);
        when(entry.getServiceId()).thenReturn(SERVICE_ID);
        when(entry.getEntry()).thenReturn(KEY_VALUE);
        when(entry.getEntryAsString()).thenThrow(new RedisEntryException("error"));

        boolean result = underTest.create(entry);
        assertFalse(result);
    }

    @Nested
    class whenUpdating {

        @Mock
        private RedisFuture<Boolean> setFuture;

        @Mock
        private RedisFuture<Boolean> existsFuture;

        @Test
        void givenExistingEntry_thenUpdateEntry() throws ExecutionException, InterruptedException, RedisOutOfMemoryException {
            when(redisCommands.hset(any(), any(), any())).thenReturn(setFuture);
            when(setFuture.get()).thenReturn(false);

            when(redisCommands.hexists(any(), any())).thenReturn(existsFuture);
            when(existsFuture.get()).thenReturn(true);

            boolean result = underTest.update(REDIS_ENTRY);
            assertTrue(result);
        }

        @Test
        void givenExistingEntryAndFailureUpdating_thenDontUpdateEntry() throws ExecutionException, InterruptedException, RedisOutOfMemoryException {
            when(redisCommands.hset(any(), any(), any())).thenReturn(setFuture);
            when(setFuture.get()).thenReturn(true);

            when(redisCommands.hexists(any(), any())).thenReturn(existsFuture);
            when(existsFuture.get()).thenReturn(true);

            boolean result = underTest.update(REDIS_ENTRY);
            assertFalse(result);
        }

        @Test
        void givenNotExistingEntry_thenDontUpdateEntry() throws ExecutionException, InterruptedException, RedisOutOfMemoryException {
            when(redisCommands.hexists(any(), any())).thenReturn(existsFuture);
            when(existsFuture.get()).thenReturn(false);

            boolean result = underTest.update(REDIS_ENTRY);
            assertFalse(result);
        }

        @Test
        void givenRedisOutOfMemory_thenThrowOutOfMemoryException() throws ExecutionException, InterruptedException {
            when(redisCommands.hset(any(), any(), any())).thenReturn(setFuture);
            when(setFuture.get()).thenThrow(new ExecutionException(new RedisCommandExecutionException("maxmemory")));

            when(redisCommands.hexists(any(), any())).thenReturn(existsFuture);
            when(existsFuture.get()).thenReturn(true);

            assertThrows(RedisOutOfMemoryException.class, () -> underTest.update(REDIS_ENTRY));
        }

        @Test
        void givenInterruptedException_thenThrowRetryException() throws ExecutionException, InterruptedException {
            when(redisCommands.hexists(any(), any())).thenReturn(existsFuture);
            when(existsFuture.get()).thenThrow(new InterruptedException());

            assertThrows(RetryableRedisException.class, () -> underTest.update(REDIS_ENTRY));
        }

        @Test
        void givenExecutionException_thenThrowRetryException() throws ExecutionException, InterruptedException {
            when(redisCommands.hexists(any(), any())).thenReturn(existsFuture);
            when(existsFuture.get()).thenThrow(new ExecutionException(new Exception()));

            assertThrows(RetryableRedisException.class, () -> underTest.update(REDIS_ENTRY));
        }

        @Test
        void givenRedisEntryException_thenReturnFalse() throws RedisEntryException, RedisOutOfMemoryException, ExecutionException, InterruptedException {
            RedisEntry entry = mock(RedisEntry.class);
            when(entry.getServiceId()).thenReturn(SERVICE_ID);
            when(entry.getEntry()).thenReturn(KEY_VALUE);
            when(entry.getEntryAsString()).thenThrow(new RedisEntryException("error"));

            when(redisCommands.hexists(any(), any())).thenReturn(existsFuture);
            when(existsFuture.get()).thenReturn(true);

            boolean result = underTest.update(entry);
            assertFalse(result);
        }
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
            when(future.get()).thenReturn(VALID_SERIALIZED_ENTRY);

            RedisEntry result = underTest.get(SERVICE_ID, KEY);
            assertThat(result.getServiceId(), is(SERVICE_ID));
            assertThat(result.getEntry().getKey(), is(KEY));
            assertThat(result.getEntry().getValue(), is(VALUE));
        }

        @Test
        void givenNotExistingKey_thenReturnNull() throws ExecutionException, InterruptedException {
            when(future.get()).thenReturn(null);

            RedisEntry result = underTest.get(SERVICE_ID, "bad key");
            assertThat(result, is(nullValue()));
        }

        @Test
        void givenKeyWithInvalidSerializedValue_thenReturnNull() throws ExecutionException, InterruptedException {
            String badSerialization = "bad key value";
            when(future.get()).thenReturn(badSerialization);

            RedisEntry result = underTest.get(SERVICE_ID, "KEY");
            assertThat(result, is(nullValue()));
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
        private RedisFuture<Map<String, String>> future;

        @BeforeEach
        void mockRedisCommand() {
            when(redisCommands.hgetall(any())).thenReturn(future);
        }

        @Test
        void givenEntries_thenReturnListOfEntries() throws ExecutionException, InterruptedException {
            Map<String, String> entries = new HashMap<>();
            entries.put(KEY, VALID_SERIALIZED_ENTRY);
            when(future.get()).thenReturn(entries);

            List<RedisEntry> result = underTest.get(SERVICE_ID);
            assertThat(result.size(), is(1));

            RedisEntry entry = result.get(0);
            assertThat(entry.getServiceId(), is(SERVICE_ID));
            assertThat(entry.getEntry(), is(KEY_VALUE));
        }

        @Test
        void givenNoEntries_thenReturnEmptyList() throws ExecutionException, InterruptedException {
            when(future.get()).thenReturn(new HashMap<>());

            List<RedisEntry> result = underTest.get(SERVICE_ID);
            assertTrue(result.isEmpty());
        }

        @Test
        void givenEntryWithInvalidSerializedValue_thenReturnValidEntries() throws ExecutionException, InterruptedException {
            Map<String, String> entries = new HashMap<>();
            entries.put(KEY, VALID_SERIALIZED_ENTRY);
            entries.put("key2", "invalid serialized value");
            when(future.get()).thenReturn(entries);

            List<RedisEntry> result = underTest.get(SERVICE_ID);
            assertThat(result.size(), is(1));

            RedisEntry entry = result.get(0);
            assertThat(entry.getServiceId(), is(SERVICE_ID));
            assertThat(entry.getEntry(), is(KEY_VALUE));
        }

        @Test
        void givenInterruptedException_thenThrowRetryException() throws ExecutionException, InterruptedException {
            when(future.get()).thenThrow(new InterruptedException());

            assertThrows(RetryableRedisException.class, () -> underTest.get(SERVICE_ID));
        }

        @Test
        void givenExecutionException_thenThrowRetryException() throws ExecutionException, InterruptedException {
            when(future.get()).thenThrow(new ExecutionException(new Exception()));

            assertThrows(RetryableRedisException.class, () -> underTest.get(SERVICE_ID));
        }
    }

    @Nested
    class whenDeleting {

        @Mock
        private RedisFuture<Long> future;

        @BeforeEach
        void mockRedisCommand() {
            when(redisCommands.hdel(any(), any())).thenReturn(future);
        }

        @Test
        void givenExistingKey_thenDeleteKey() throws ExecutionException, InterruptedException {
            when(future.get()).thenReturn((long) 1);
            boolean result = underTest.delete(SERVICE_ID, KEY);
            assertTrue(result);
        }

        @Test
        void givenNotExistingKey_thenDontDeleteKey() throws ExecutionException, InterruptedException {
            when(future.get()).thenReturn((long) 0);
            boolean result = underTest.delete(SERVICE_ID, KEY);
            assertFalse(result);
        }

        @Test
        void givenInterruptedException_thenThrowRetryException() throws ExecutionException, InterruptedException {
            when(future.get()).thenThrow(new InterruptedException());

            assertThrows(RetryableRedisException.class, () -> underTest.delete(SERVICE_ID, KEY));
        }

        @Test
        void givenExecutionException_thenThrowRetryException() throws ExecutionException, InterruptedException {
            when(future.get()).thenThrow(new ExecutionException(new Exception()));

            assertThrows(RetryableRedisException.class, () -> underTest.delete(SERVICE_ID, KEY));
        }
    }

    @Nested
    class whenDeletingAllEntries {

        @Mock
        private RedisFuture<Long> future;

        @BeforeEach
        void mockRedisCommand() {
            when(redisCommands.del(any())).thenReturn(future);
        }

        @Test
        void givenExistingEntries_thenDeleteAllEntries() throws ExecutionException, InterruptedException {
            when(future.get()).thenReturn((long) 2);
            boolean result = underTest.delete(SERVICE_ID);
            assertTrue(result);
        }

        @Test
        void givenNoEntries_thenReturnFalse() throws ExecutionException, InterruptedException {
            when(future.get()).thenReturn((long) 0);
            boolean result = underTest.delete(SERVICE_ID);
            assertFalse(result);
        }

        @Test
        void givenInterruptedException_thenThrowRetryException() throws ExecutionException, InterruptedException {
            when(future.get()).thenThrow(new InterruptedException());

            assertThrows(RetryableRedisException.class, () -> underTest.delete(SERVICE_ID));
        }

        @Test
        void givenExecutionException_thenThrowRetryException() throws ExecutionException, InterruptedException {
            when(future.get()).thenThrow(new ExecutionException(new Exception()));

            assertThrows(RetryableRedisException.class, () -> underTest.delete(SERVICE_ID));
        }
    }

    @Test
    void givenRedisExecutionExceptionNotOutOfMemory_thenThrowRetryableRedisException() throws ExecutionException, InterruptedException {
        RedisFuture<Boolean> future = (RedisFuture<Boolean>) mock(RedisFuture.class);
        when(redisCommands.hsetnx(any(), any(), any())).thenReturn(future);
        when(future.get()).thenThrow(new ExecutionException(new RedisCommandExecutionException("error")));

        assertThrows(RetryableRedisException.class, () -> underTest.create(REDIS_ENTRY));
    }

    @Nested
    class WhenDestroying {

        @Test
        void givenConnectionAndClient_thenCloseConnectionAndClient() {
            underTest = new RedisOperator(redisClient, redisConnection, redisCommands);
            underTest.closeConnection();

            verify(redisConnection, times(1)).close();
            verify(redisClient, times(1)).shutdown();
        }

        @Test
        void givenConnection_thenCloseConnectionAndNotClient() {
            underTest = new RedisOperator(null, redisConnection, redisCommands);
            underTest.closeConnection();

            verify(redisConnection, times(1)).close();
            verify(redisClient, times(0)).shutdown();
        }
        @Test
        void givenClient_thenCloseClientAndNotConnection() {
            underTest = new RedisOperator(redisClient, null, redisCommands);
            underTest.closeConnection();

            verify(redisConnection, times(0)).close();
            verify(redisClient, times(1)).shutdown();
        }
        @Test
        void givenNoConnectionOrClient_thenCloseNothing() {
            underTest = new RedisOperator(null, null, redisCommands);
            underTest.closeConnection();

            verify(redisConnection, times(0)).close();
            verify(redisClient, times(0)).shutdown();
        }
    }
}
