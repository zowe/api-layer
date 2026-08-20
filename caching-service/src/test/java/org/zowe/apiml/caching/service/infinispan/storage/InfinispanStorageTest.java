/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.service.infinispan.storage;

import org.infinispan.Cache;
import org.infinispan.CacheSet;
import org.infinispan.commons.util.IteratorMapper;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.zowe.apiml.cache.StorageException;
import org.zowe.apiml.caching.model.KeyValue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.cache.PatRevocationStore.INVALID_SCOPES_KEY;
import static org.zowe.apiml.cache.PatRevocationStore.INVALID_TOKENS_KEY;
import static org.zowe.apiml.cache.PatRevocationStore.INVALID_USERS_KEY;
import static org.zowe.apiml.cache.PatRevocationStore.RULE_RETENTION_DAYS;
import static org.zowe.apiml.caching.service.infinispan.config.InfinispanConfig.CACHE_ZOWE;
import static org.zowe.apiml.caching.service.infinispan.config.InfinispanConfig.CACHE_ZOWE_INVALIDATED_TOKEN;
import static org.zowe.apiml.caching.service.infinispan.config.InfinispanConfig.CACHE_ZOWE_INVALIDATED_TOKEN_ITEM;

class InfinispanStorageTest {

    public static final KeyValue TO_CREATE = new KeyValue("key1", "val1");
    public static final KeyValue TO_UPDATE = new KeyValue("key1", "val2");

    private static final long MAX_TTL_SECONDS = Duration.ofDays(RULE_RETENTION_DAYS).toSeconds();

    Cache<String, KeyValue> cache;
    Cache<String, String> tokenCache;
    Cache<String, Map<String, String>> legacyTokenCache;
    InfinispanStorage storage;
    String serviceId1 = "service1";
    String serviceId2 = "service2";

    @BeforeEach
    void setup() {
        cache = mock(Cache.class);
        tokenCache = mock(Cache.class);
        legacyTokenCache = mock(Cache.class);
        storage = newStorage(cache, tokenCache, legacyTokenCache);
    }

    private InfinispanStorage newStorage(Cache<String, KeyValue> cache, Cache<String, String> tokenCache, Cache<String, Map<String, String>> legacyCache) {
        return new InfinispanStorage(createCacheManager(cache, tokenCache, legacyCache), MAX_TTL_SECONDS, 0);
    }

    private DefaultCacheManager createCacheManager(
        Cache<String, KeyValue> cache,
        Cache<String, String> tokenCache,
        Cache<String, Map<String, String>> legacyCache
    ) {
        var defaultCacheManager = mock(DefaultCacheManager.class);
        doReturn(cache).when(defaultCacheManager).getCache(CACHE_ZOWE);
        doReturn(tokenCache).when(defaultCacheManager).getCache(CACHE_ZOWE_INVALIDATED_TOKEN_ITEM);
        doReturn(legacyCache).when(defaultCacheManager).getCache(CACHE_ZOWE_INVALIDATED_TOKEN);
        return defaultCacheManager;
    }

    private static String itemKey(String serviceId, String mapKey, String key) {
        return serviceId.length() + "|" + serviceId + mapKey.length() + "|" + mapKey + key;
    }

    private static String tokenRecord(LocalDateTime expiresAt) {
        String expiry = expiresAt == null
            ? "null"
            : String.format("[%d,%d,%d,%d,%d,%d]", expiresAt.getYear(), expiresAt.getMonthValue(), expiresAt.getDayOfMonth(),
                expiresAt.getHour(), expiresAt.getMinute(), expiresAt.getSecond());
        return "{\"userId\":null,\"tokenValue\":\"hashedKey\",\"issuedAt\":[2022,8,17,16,13,18],\"expiresAt\":" + expiry + ",\"scopes\":null,\"tokenProvider\":null}";
    }

    @Nested
    class WhenEntryDoesntExist {

        KeyValue keyValue;

        @BeforeEach
        void createEmptyStore() {
            keyValue = null;
        }

        @Test
        void whenRead_thenExceptionIsThrown() {
            String key = TO_CREATE.getKey();
            when(cache.get(serviceId1)).thenReturn(keyValue);
            assertThrows(StorageException.class, () -> storage.read(serviceId1, key));
        }

        @Test
        void whenUpdate_thenExceptionIsThrown() {
            KeyValue entry = new KeyValue("key", "value");
            when(cache.get(serviceId1)).thenReturn(keyValue);
            assertThrows(StorageException.class, () -> storage.update(serviceId1, entry));
        }

        @Test
        void whenAddNew_returnNull() {
            keyValue = new KeyValue("key", "value");
            assertNull(storage.create(serviceId1, keyValue));
        }

        @Test
        void whenDelete_thenExceptionIsThrown() {

            String key = TO_CREATE.getKey();
            when(cache.remove(serviceId1 + key)).thenReturn(null);
            assertThrows(StorageException.class, () -> storage.delete(serviceId1, key));
        }

    }

    @Nested
    class WhenEntryExists {

        KeyValue keyValue;

        @BeforeEach
        void createStoreWithEntry() {
            keyValue = TO_CREATE;
        }

        @Test
        void exceptionIsThrown() {
            when(cache.putIfAbsent(any(), any())).thenReturn(keyValue);
            assertThrows(StorageException.class, () -> storage.create(serviceId1, TO_CREATE));
        }

        @Test
        void entryIsReturned() {
            when(cache.get(serviceId1 + TO_CREATE.getKey())).thenReturn(TO_CREATE);
            KeyValue result = storage.read(serviceId1, TO_CREATE.getKey());
            assertEquals(TO_CREATE.getValue(), result.getValue());
        }

        @Test
        void cacheIsUpdated() {

            when(cache.put(serviceId1 + TO_UPDATE.getKey(), TO_UPDATE)).thenReturn(TO_UPDATE);
            storage.update(serviceId1, TO_UPDATE);
            verify(cache, times(1)).put(serviceId1 + TO_UPDATE.getKey(), TO_UPDATE);
            assertEquals("val2", TO_UPDATE.getValue());
        }

        @Test
        void itemIsDeleted() {
            Cache<String, KeyValue> cache = createCache();
            InfinispanStorage storage = newStorage(cache, tokenCache, legacyTokenCache);
            assertNull(storage.create(serviceId1, TO_CREATE));
            assertEquals(TO_CREATE, storage.delete(serviceId1, TO_CREATE.getKey()));
        }

        @Test
        void returnAll() {
            Cache<String, KeyValue> cache = createCache();
            InfinispanStorage storage = newStorage(cache, tokenCache, legacyTokenCache);
            storage.create(serviceId1, new KeyValue("key", "value"));
            storage.create(serviceId1, new KeyValue("key2", "value2"));
            assertEquals(2, storage.readForService(serviceId1).size());
        }

        @Test
        void removeAll() {
            Cache<String, KeyValue> cache = createCache();
            InfinispanStorage storage = newStorage(cache, tokenCache, legacyTokenCache);
            storage.create(serviceId1, new KeyValue("key", "value"));
            storage.create(serviceId1, new KeyValue("key2", "value2"));
            assertEquals(2, storage.readForService(serviceId1).size());
            storage.deleteForService(serviceId1);
            assertEquals(0, storage.readForService(serviceId1).size());
        }

    }

    @Nested
    class WhenStoreToken {

        @Test
        void thenOneEntryPerItemIsWritten() {
            assertNull(storage.storeMapItem(serviceId1, INVALID_TOKENS_KEY, new KeyValue("newkey", "newvalue", 60L)));
            verify(tokenCache).put(itemKey(serviceId1, INVALID_TOKENS_KEY, "newkey"), "newvalue", 60L, TimeUnit.SECONDS);
        }

        @Test
        void givenAnotherItemOfTheSameMap_thenTheFirstOneIsNotTouched() {
            storage.storeMapItem(serviceId1, INVALID_TOKENS_KEY, new KeyValue("keyA", "a", 60L));
            storage.storeMapItem(serviceId1, INVALID_TOKENS_KEY, new KeyValue("keyB", "b", 60L));

            verify(tokenCache).put(itemKey(serviceId1, INVALID_TOKENS_KEY, "keyA"), "a", 60L, TimeUnit.SECONDS);
            verify(tokenCache).put(itemKey(serviceId1, INVALID_TOKENS_KEY, "keyB"), "b", 60L, TimeUnit.SECONDS);
            verify(tokenCache, never()).get(any());
        }

        @Test
        void givenNoRequestedTtl_thenItIsDerivedFromTheTokenRecord() {
            storage.storeMapItem(serviceId1, INVALID_TOKENS_KEY,
                new KeyValue("k", tokenRecord(LocalDateTime.now().plusDays(2))));

            ArgumentCaptor<Long> ttl = ArgumentCaptor.forClass(Long.class);
            verify(tokenCache).put(any(), any(), ttl.capture(), eq(TimeUnit.SECONDS));
            assertTrue(ttl.getValue() > Duration.ofDays(1).toSeconds());
            assertTrue(ttl.getValue() <= Duration.ofDays(2).toSeconds());
        }

        @Test
        void givenNoRequestedTtl_thenARuleExpiresAfterTheRetentionPeriod() {
            long now = System.currentTimeMillis();
            storage.storeMapItem(serviceId1, INVALID_USERS_KEY, new KeyValue("k", Long.toString(now)));

            ArgumentCaptor<Long> ttl = ArgumentCaptor.forClass(Long.class);
            verify(tokenCache).put(any(), any(), ttl.capture(), eq(TimeUnit.SECONDS));
            assertTrue(ttl.getValue() > Duration.ofDays(RULE_RETENTION_DAYS - 1).toSeconds());
            assertTrue(ttl.getValue() <= MAX_TTL_SECONDS);
        }

        /**
         * A negative lifespan would mean "never expire" to Infinispan, so a rule that is already past its
         * retention has to be removed rather than stored - otherwise the oldest entries are the immortal ones.
         */
        @Test
        void givenAnAlreadyIrrelevantRule_thenItIsRemovedInsteadOfStored() {
            storage.storeMapItem(serviceId1, INVALID_SCOPES_KEY, new KeyValue("k", "1582239600000"));

            verify(tokenCache).remove(itemKey(serviceId1, INVALID_SCOPES_KEY, "k"));
            verify(tokenCache, never()).put(any(), any(), anyLong(), any());
        }

        @Test
        void givenAnAlreadyExpiredToken_thenItIsRemovedInsteadOfStored() {
            storage.storeMapItem(serviceId1, INVALID_TOKENS_KEY,
                new KeyValue("k", tokenRecord(LocalDateTime.now().minusDays(1))));

            verify(tokenCache).remove(itemKey(serviceId1, INVALID_TOKENS_KEY, "k"));
            verify(tokenCache, never()).put(any(), any(), anyLong(), any());
        }

        @Test
        void givenAnUnknownMapAndAnUninterpretableValue_thenItIsStoredWithTheCeilingTtl() {
            storage.storeMapItem(serviceId1, "aMap", new KeyValue("aMapCacheKey", "aMapCacheValue"));

            verify(tokenCache).put(itemKey(serviceId1, "aMap", "aMapCacheKey"), "aMapCacheValue", MAX_TTL_SECONDS, TimeUnit.SECONDS);
        }

        @Test
        void givenARequestedTtlAboveTheCeiling_thenItIsClamped() {
            storage.storeMapItem(serviceId1, INVALID_TOKENS_KEY, new KeyValue("k", "v", MAX_TTL_SECONDS * 10));

            verify(tokenCache).put(any(), any(), eq(MAX_TTL_SECONDS), eq(TimeUnit.SECONDS));
        }
    }

    @Nested
    class WhenRetrieveToken {

        @Test
        void returnTokenList() {
            Cache<String, String> tokenCache = createCache();
            InfinispanStorage storage = newStorage(cache, tokenCache, legacyTokenCache);
            tokenCache.put(itemKey(serviceId1, INVALID_TOKENS_KEY, "key1"), "token1");
            tokenCache.put(itemKey(serviceId1, INVALID_TOKENS_KEY, "key2"), "token2");

            Map<String, String> result = storage.getAllMapItems(serviceId1, INVALID_TOKENS_KEY);

            assertEquals(2, result.size());
            assertEquals("token1", result.get("key1"));
            assertEquals("token2", result.get("key2"));
        }

        @Test
        void givenNoItems_thenReturnEmptyMap() {
            Cache<String, String> tokenCache = createCache();
            InfinispanStorage storage = newStorage(cache, tokenCache, legacyTokenCache);

            assertTrue(storage.getAllMapItems(serviceId1, INVALID_TOKENS_KEY).isEmpty());
        }
    }

    @Nested
    class WhenRetrieveInvalidTokensAndRules {

        InfinispanStorage underTest;
        Cache<String, String> tokenCache;

        @BeforeEach
        void createStorage() {
            tokenCache = createCache();
            tokenCache.put(itemKey(serviceId1, INVALID_TOKENS_KEY, "key1"), "token1");
            tokenCache.put(itemKey(serviceId1, INVALID_TOKENS_KEY, "key2"), "token2");
            tokenCache.put(itemKey(serviceId1, "invalidTokenRules", "key1"), "rule1");
            tokenCache.put(itemKey(serviceId1, "invalidTokenRules", "key2"), "rule2");
            tokenCache.put(itemKey(serviceId2, INVALID_TOKENS_KEY, "key3"), "token3");
            underTest = newStorage(cache, tokenCache, legacyTokenCache);
        }

        @Test
        void returnAllForGivenService() {
            Map<String, Map<String, String>> result = underTest.getAllMaps(serviceId1);

            assertEquals(2, result.size());
            assertNotNull(result.get(INVALID_TOKENS_KEY));
            assertEquals(2, result.get(INVALID_TOKENS_KEY).size());
            assertNotNull(result.get("invalidTokenRules"));
            assertEquals(2, result.get("invalidTokenRules").size());
        }

        @Test
        void returnAllForAnotherService() {
            Map<String, Map<String, String>> result = underTest.getAllMaps(serviceId2);

            assertEquals(1, result.size());
            assertNotNull(result.get(INVALID_TOKENS_KEY));
            assertEquals(1, result.get(INVALID_TOKENS_KEY).size());
            assertNull(result.get("invalidTokenRules"));
        }

        @Test
        void returnNoneForUnknownService() {
            Map<String, Map<String, String>> result = underTest.getAllMaps("unknown_service");

            assertEquals(0, result.size());
        }

        /**
         * The previous bare-concatenated keys made one service id that is a prefix of another leak entries
         * across services; the length prefix is what removes the ambiguity.
         */
        @Test
        void givenOneServiceIdIsAPrefixOfAnother_thenEntriesDoNotLeak() {
            tokenCache.put(itemKey("service", INVALID_TOKENS_KEY, "other"), "otherToken");

            Map<String, Map<String, String>> result = underTest.getAllMaps("service");

            assertEquals(1, result.size());
            assertEquals(Map.of("other", "otherToken"), result.get(INVALID_TOKENS_KEY));
        }
    }

    @Nested
    class WhenQueryingSpecificItems {

        InfinispanStorage underTest;
        Cache<String, String> tokenCache;

        @BeforeEach
        void createStorage() {
            tokenCache = createCache();
            tokenCache.put(itemKey(serviceId1, INVALID_TOKENS_KEY, "tokenHash"), "tokenRecord");
            tokenCache.put(itemKey(serviceId1, INVALID_USERS_KEY, "userHash"), "1595282400000");
            underTest = newStorage(cache, tokenCache, legacyTokenCache);
        }

        @Test
        void thenOnlyTheFoundEntriesAreReturned() {
            Map<String, Collection<String>> query = new HashMap<>();
            query.put(INVALID_TOKENS_KEY, List.of("tokenHash", "unknownHash"));
            query.put(INVALID_USERS_KEY, List.of("userHash"));
            query.put(INVALID_SCOPES_KEY, List.of("scopeHash"));

            Map<String, Map<String, String>> result = underTest.getMapItems(serviceId1, query);

            assertEquals(Map.of("tokenHash", "tokenRecord"), result.get(INVALID_TOKENS_KEY));
            assertEquals(Map.of("userHash", "1595282400000"), result.get(INVALID_USERS_KEY));
            assertNull(result.get(INVALID_SCOPES_KEY), "a map with nothing found should be omitted entirely");
        }

        @Test
        void givenAnEmptyQuery_thenNothingIsRead() {
            assertTrue(underTest.getMapItems(serviceId1, Map.of()).isEmpty());
            assertTrue(underTest.getMapItems(serviceId1, null).isEmpty());
        }

        @Test
        void givenAnotherService_thenNothingIsFound() {
            Map<String, Collection<String>> query = Map.of(INVALID_TOKENS_KEY, List.of("tokenHash"));
            assertTrue(underTest.getMapItems(serviceId2, query).isEmpty());
        }
    }

    @Nested
    class WhenReadingTheLegacyStore {

        @Test
        void thenTheWholeMapLayoutIsReturned() {
            Cache<String, Map<String, String>> legacy = createCache();
            legacy.put(serviceId1 + INVALID_TOKENS_KEY, Map.of("key1", "token1"));
            legacy.put(serviceId2 + INVALID_TOKENS_KEY, Map.of("key2", "token2"));
            InfinispanStorage underTest = newStorage(cache, tokenCache, legacy);

            Map<String, Map<String, String>> result = underTest.getAllLegacyMaps(serviceId1);

            assertEquals(1, result.size());
            assertEquals(Map.of("key1", "token1"), result.get(INVALID_TOKENS_KEY));
        }

        @Test
        void thenTheNewLayoutIsNotTouched() {
            Cache<String, Map<String, String>> legacy = createCache();
            InfinispanStorage underTest = newStorage(cache, tokenCache, legacy);

            assertTrue(underTest.getAllLegacyMaps(serviceId1).isEmpty());
            verify(tokenCache, never()).keySet();
        }
    }

    @Nested
    class WhenEvictNonRelevantTokensAndRules {

        InfinispanStorage underTest;
        Cache<String, String> tokenCache;

        @BeforeEach
        void createStorage() {
            tokenCache = createCache();
            tokenCache.put(itemKey(serviceId1, INVALID_TOKENS_KEY, "expired"), tokenRecord(LocalDateTime.now().minusDays(1)));
            tokenCache.put(itemKey(serviceId1, INVALID_TOKENS_KEY, "live"), tokenRecord(LocalDateTime.now().plusDays(1)));
            tokenCache.put(itemKey(serviceId1, INVALID_SCOPES_KEY, "old"), "1595282400000");
            tokenCache.put(itemKey(serviceId1, INVALID_USERS_KEY, "old"), "1595282400000");
            tokenCache.put(itemKey(serviceId1, INVALID_USERS_KEY, "fresh"), Long.toString(System.currentTimeMillis()));
            underTest = newStorage(cache, tokenCache, legacyTokenCache);
        }

        @Test
        void thenEvictItems() {
            underTest.removeNonRelevantTokens(serviceId1, INVALID_TOKENS_KEY);
            underTest.removeNonRelevantRules(serviceId1, INVALID_SCOPES_KEY);
            underTest.removeNonRelevantRules(serviceId1, INVALID_USERS_KEY);

            Map<String, Map<String, String>> result = underTest.getAllMaps(serviceId1);
            assertEquals(1, result.get(INVALID_TOKENS_KEY).size());
            assertNotNull(result.get(INVALID_TOKENS_KEY).get("live"));
            assertNull(result.get(INVALID_SCOPES_KEY));
            assertEquals(1, result.get(INVALID_USERS_KEY).size());
            assertNotNull(result.get(INVALID_USERS_KEY).get("fresh"));
        }

        /**
         * A single record that cannot be interpreted used to abort the whole read-filter-write, so one poison
         * entry blocked cleanup of its entire map on every cycle. It must be skipped, not thrown on - and
         * kept rather than dropped, since a parse error is not evidence that a token is no longer revoked.
         */
        @Test
        void givenAPoisonRecord_thenTheRestOfTheMapIsStillCleaned() {
            tokenCache.put(itemKey(serviceId1, INVALID_TOKENS_KEY, "unparseable"), "not json at all");
            tokenCache.put(itemKey(serviceId1, INVALID_TOKENS_KEY, "noExpiry"), tokenRecord(null));
            tokenCache.put(itemKey(serviceId1, INVALID_USERS_KEY, "unparseable"), "not a number");

            assertDoesNotThrow(() -> underTest.removeNonRelevantTokens(serviceId1, INVALID_TOKENS_KEY));
            assertDoesNotThrow(() -> underTest.removeNonRelevantRules(serviceId1, INVALID_USERS_KEY));

            Map<String, Map<String, String>> result = underTest.getAllMaps(serviceId1);
            assertNull(result.get(INVALID_TOKENS_KEY).get("expired"), "the expired record was still cleaned up");
            assertNotNull(result.get(INVALID_TOKENS_KEY).get("unparseable"));
            assertNotNull(result.get(INVALID_TOKENS_KEY).get("noExpiry"));
            assertNotNull(result.get(INVALID_USERS_KEY).get("unparseable"));
            assertNull(result.get(INVALID_USERS_KEY).get("old"), "the stale rule was still cleaned up");
        }

        @Test
        void thenOtherServicesAreUntouched() {
            tokenCache.put(itemKey(serviceId2, INVALID_TOKENS_KEY, "expired"), tokenRecord(LocalDateTime.now().minusDays(1)));

            underTest.removeNonRelevantTokens(serviceId1, INVALID_TOKENS_KEY);

            assertEquals(1, underTest.getAllMaps(serviceId2).get(INVALID_TOKENS_KEY).size());
        }
    }

    @Nested
    class WhenTheStoreGrows {

        /**
         * The warning has to survive a revocation burst without becoming the noise it is warning about, so it
         * fires at most once per doubling rather than once per write.
         */
        @Test
        void thenTheWarningIsThrottledToOncePerDoubling() {
            Cache<String, String> tokenCache = createCache();
            var sizes = new java.util.concurrent.atomic.AtomicInteger(10);
            doAnswer(a -> sizes.get()).when(tokenCache).size();

            var apimlLog = mock(org.zowe.apiml.message.log.ApimlLogger.class);
            InfinispanStorage underTest = new InfinispanStorage(createCacheManager(cache, tokenCache, legacyTokenCache), MAX_TTL_SECONDS, 10, 1);
            org.springframework.test.util.ReflectionTestUtils.setField(underTest, "apimlLog", apimlLog);

            for (int i = 0; i < 5; i++) {
                underTest.storeMapItem(serviceId1, INVALID_TOKENS_KEY, new KeyValue("k" + i, "v", 60L));
            }
            verify(apimlLog, times(1)).log(eq("org.zowe.apiml.cache.revocationStoreTooLarge"), any(), any());

            sizes.set(20);
            underTest.storeMapItem(serviceId1, INVALID_TOKENS_KEY, new KeyValue("kDoubled", "v", 60L));
            verify(apimlLog, times(2)).log(eq("org.zowe.apiml.cache.revocationStoreTooLarge"), any(), any());
        }

        @Test
        void givenNoThreshold_thenNothingIsLogged() {
            Cache<String, String> tokenCache = createCache();
            var apimlLog = mock(org.zowe.apiml.message.log.ApimlLogger.class);
            InfinispanStorage underTest = new InfinispanStorage(createCacheManager(cache, tokenCache, legacyTokenCache), MAX_TTL_SECONDS, 0, 1);
            org.springframework.test.util.ReflectionTestUtils.setField(underTest, "apimlLog", apimlLog);

            underTest.storeMapItem(serviceId1, INVALID_TOKENS_KEY, new KeyValue("k", "v", 60L));

            verify(apimlLog, never()).log(eq("org.zowe.apiml.cache.revocationStoreTooLarge"), any(), any());
        }

        /**
         * Sampled on write rather than read on demand, so that a metrics scrape cannot walk the persistent
         * store.
         */
        @Test
        void thenTheSampledSizeIsExposedForMetrics() {
            Cache<String, String> tokenCache = createCache();
            InfinispanStorage underTest = new InfinispanStorage(createCacheManager(cache, tokenCache, legacyTokenCache), MAX_TTL_SECONDS, 0, 1);

            assertEquals(-1, underTest.getLastObservedRevocationStoreSize(), "nothing sampled yet");

            underTest.storeMapItem(serviceId1, INVALID_TOKENS_KEY, new KeyValue("k", "v", 60L));

            assertEquals(1, underTest.getLastObservedRevocationStoreSize());
        }

        @Test
        void givenWritesBetweenSamples_thenTheSizeIsNotReadEveryTime() {
            Cache<String, String> tokenCache = createCache();
            InfinispanStorage underTest = new InfinispanStorage(createCacheManager(cache, tokenCache, legacyTokenCache), MAX_TTL_SECONDS, 0, 100);

            for (int i = 0; i < 10; i++) {
                underTest.storeMapItem(serviceId1, INVALID_TOKENS_KEY, new KeyValue("k" + i, "v", 60L));
            }

            verify(tokenCache, never()).size();
            assertEquals(-1, underTest.getLastObservedRevocationStoreSize());
        }
    }

    @Nested
    class WhenEncodingKeys {

        @Test
        void thenTheKeyRoundTrips() {
            String key = InfinispanStorage.encodeItemKey("CN=zowe, OU=API|3", "3|weird", "hash");
            String prefix = InfinispanStorage.encodeServicePrefix("CN=zowe, OU=API|3");

            assertTrue(key.startsWith(prefix));
            assertArrayEquals(new String[]{"3|weird", "hash"}, InfinispanStorage.decodeAfterService(key, prefix.length()));
        }

        @Test
        void givenAKeyThatIsNotLengthPrefixed_thenDecodingReturnsNull() {
            assertNull(InfinispanStorage.decodeAfterService("noSeparator", 0));
            assertNull(InfinispanStorage.decodeAfterService("notANumber|x", 0));
            assertNull(InfinispanStorage.decodeAfterService("99|tooShort", 0));
        }
    }

    private <K, V> Cache<K, V> createCache() {
        var data = new HashMap<K, V>();
        var cache = mock(Cache.class);
        doAnswer(answer -> data.put(answer.getArgument(0), answer.getArgument(1))).when(cache).put(any(), any());
        doAnswer(answer -> data.put(answer.getArgument(0), answer.getArgument(1))).when(cache).put(any(), any(), anyLong(), any());
        doAnswer(answer -> data.putIfAbsent(answer.getArgument(0), answer.getArgument(1))).when(cache).putIfAbsent(any(), any());
        doAnswer(answer -> data.get(answer.getArgument(0))).when(cache).get(any());
        doAnswer(answer -> data.remove(answer.getArgument(0))).when(cache).remove(any());
        doAnswer(answer -> data.remove(answer.getArgument(0), answer.getArgument(1))).when(cache).remove(any(), any());
        doAnswer(answer -> data.size()).when(cache).size();
        doAnswer(answer -> {
            new HashMap<>(data).forEach(answer.getArgument(0));
            return null;
        }).when(cache).forEach(any());
        doAnswer(answer -> createCacheSet(new java.util.LinkedHashSet<>(data.keySet()))).when(cache).keySet();
        return cache;
    }

    private <K> CacheSet<K> createCacheSet(Set<K> keys) {
        var set = mock(CacheSet.class);
        doReturn(new IteratorMapper(keys.iterator(), Function.identity())).when(set).iterator();
        return set;
    }

}
