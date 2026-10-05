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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.zowe.apiml.cache.Storage;
import org.zowe.apiml.cache.StorageException;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Messages;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.models.AccessTokenContainer;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import static org.zowe.apiml.cache.PatRevocationStore.*;
import static org.zowe.apiml.caching.service.infinispan.config.InfinispanConfig.*;

@Slf4j
public class InfinispanStorage implements Storage {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * How often the size of the revocation store is sampled, in writes. Sampling keeps the (potentially
     * store-touching) {@code size()} call off every single revocation.
     */
    private int DEFAULT_SIZE_CHECK_INTERVAL = 1000;

    /**
     * Lifespan for "store without expiration". Deliberately not {@code -1}, which is what Infinispan
     * itself uses for that, because {@code -1} is indistinguishable from an ordinary already-elapsed
     * retention - and those must be removed rather than stored forever.
     */
    private static final long NO_EXPIRY = Long.MIN_VALUE;

    private final DefaultCacheManager defaultCacheManager;
    private final long maxTtlSeconds;
    private final long sizeWarningThreshold;

    private final AtomicLong writeCounter = new AtomicLong();
    private final AtomicLong lastWarnedSize = new AtomicLong();
    private final AtomicLong lastObservedSize = new AtomicLong(-1);

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    public InfinispanStorage(DefaultCacheManager defaultCacheManager, long maxTtlSeconds, long sizeWarningThreshold) {
        this.defaultCacheManager = defaultCacheManager;
        this.maxTtlSeconds = maxTtlSeconds;
        this.sizeWarningThreshold = sizeWarningThreshold;
    }

    private ConcurrentMap<String, KeyValue> getCache() {
        return defaultCacheManager.getCache(CACHE_ZOWE);
    }

    private Cache<String, String> getTokenItemCache() {
        return defaultCacheManager.getCache(CACHE_ZOWE_INVALIDATED_TOKEN_ITEM);
    }

    /**
     * @deprecated read-only access to the pre-cutover layout; removed together with the legacy read path.
     */
    @Deprecated(since = "3.6.0") // scheduled for removal with the legacy read path
    private Cache<String, Map<String, String>> getLegacyTokenCache() {
        return defaultCacheManager.getCache(CACHE_ZOWE_INVALIDATED_TOKEN);
    }

    @Override
    public KeyValue create(String serviceId, KeyValue toCreate) {
        toCreate.setServiceId(serviceId);
        log.info("Writing record: {}|{}|{}", serviceId, toCreate.getKey(), toCreate.getValue());

        KeyValue serviceCache = getCache().putIfAbsent(serviceId + toCreate.getKey(), toCreate);

        if (serviceCache != null) {
            throw new StorageException(Messages.DUPLICATE_KEY.getKey(), Messages.DUPLICATE_KEY.getStatus(), toCreate.getKey());
        }
        return null;
    }

    @Override
    public KeyValue storeMapItem(String serviceId, String mapKey, KeyValue toCreate) {
        String cacheKey = encodeItemKey(serviceId, mapKey, toCreate.getKey());
        Cache<String, String> cache = getTokenItemCache();
        long ttlSeconds = resolveTtlSeconds(mapKey, toCreate);

        if (ttlSeconds == NO_EXPIRY) {
            log.debug("Storing item into the cache: {}|{}, without expiration", mapKey, toCreate.getKey());
            cache.put(cacheKey, toCreate.getValue());
            warnIfStoreTooLarge(cache);
            return null;
        }

        if (ttlSeconds <= 0) {
            log.debug("Item {} of map {} is already past its retention, removing instead of storing", toCreate.getKey(), mapKey);
            cache.remove(cacheKey);
            return null;
        }

        log.debug("Storing item into the cache: {}|{}, expiring in {}s", mapKey, toCreate.getKey(), ttlSeconds);
        cache.put(cacheKey, toCreate.getValue(), ttlSeconds, TimeUnit.SECONDS);
        warnIfStoreTooLarge(cache);
        return null;
    }

    @Override
    public Map<String, String> getAllMapItems(String serviceId, String mapKey) {
        log.debug("Reading all records from cache for service {} under the {} key.", serviceId, mapKey);
        String prefix = encodeMapPrefix(serviceId, mapKey);
        Cache<String, String> cache = getTokenItemCache();

        Map<String, String> result = new HashMap<>(legacyMapItems(serviceId, mapKey));
        for (String key : cache.keySet()) {
            if (!key.startsWith(prefix)) continue;
            String value = cache.get(key);
            if (value != null) {
                result.put(key.substring(prefix.length()), value);
            }
        }
        return result;
    }

    @Override
    public Map<String, Map<String, String>> getAllMaps(String serviceId) {
        log.debug("Reading all records from cache for service {} ", serviceId);
        String prefix = encodeServicePrefix(serviceId);
        Cache<String, String> cache = getTokenItemCache();

        Map<String, Map<String, String>> result = new HashMap<>();
        getAllLegacyMaps(serviceId).forEach((legacyMapKey, items) -> result.put(legacyMapKey, new HashMap<>(items)));

        for (String key : cache.keySet()) {
            if (!key.startsWith(prefix)) continue;
            String[] mapAndItem = decodeAfterService(key, prefix.length());
            if (mapAndItem == null) {
                log.debug("Skipping undecodable key in the cache");
                continue;
            }
            String value = cache.get(key);
            if (value != null) {
                result.computeIfAbsent(mapAndItem[0], k -> new HashMap<>()).put(mapAndItem[1], value);
            }
        }
        return result;
    }

    /**
     * The pre-cutover items of one map, or empty when there are none.
     *
     * @deprecated goes away with the legacy read path, at which point the overlay goes with it.
     */
    @Deprecated(since = "3.6.0") // scheduled for removal with the legacy read path
    private Map<String, String> legacyMapItems(String serviceId, String mapKey) {
        Map<String, String> legacy = getLegacyTokenCache().get(serviceId + mapKey);
        return legacy == null ? Map.of() : legacy;
    }

    @Override
    public Map<String, Map<String, String>> getMapItems(String serviceId, Map<String, Collection<String>> keysByMapKey) {
        Map<String, Map<String, String>> result = new HashMap<>();
        if (keysByMapKey == null || keysByMapKey.isEmpty()) {
            return result;
        }
        Cache<String, String> cache = getTokenItemCache();
        for (Map.Entry<String, Collection<String>> requested : keysByMapKey.entrySet()) {
            if (hasNulls(requested)) continue;
            var found = findByKeys(cache, serviceId, requested.getKey(), requested.getValue());
            if (!found.isEmpty()) {
                result.put(requested.getKey(), found);
            }
        }
        return result;
    }

    private boolean hasNulls(Map.Entry<String, Collection<String>> requested) {
        return requested.getKey() == null || requested.getValue() == null;
    }

    private Map<String, String> findByKeys(Cache<String, String> cache, String serviceId, String mapKey, Collection<String> keys) {
        Map<String, String> found = new HashMap<>();
        for (String itemKey : keys) {
            if (itemKey == null) continue;
            String value = cache.get(encodeItemKey(serviceId, mapKey, itemKey));
            if (value != null) {
                found.put(itemKey, value);
            }
        }
        return found;
    }

    /**
     * @param serviceId Id of the service to load the legacy maps for
     * @deprecated scheduled for removal with the legacy read path
     */
    @Override
    @Deprecated(since = "3.6.0")
    @SuppressWarnings("java:S1133") // the deprecation is the point: this method exists in order to be deleted
    public Map<String, Map<String, String>> getAllLegacyMaps(String serviceId) {
        log.debug("Reading all records from the legacy cache for service {}", serviceId);
        ConcurrentMap<String, Map<String, String>> legacy = getLegacyTokenCache();

        Map<String, Map<String, String>> result = new HashMap<>();
        for (String key : legacy.keySet()) {
            if (!key.startsWith(serviceId)) continue;
            Map<String, String> value = legacy.get(key);
            if (value != null) {
                result.put(key.substring(serviceId.length()), value);
            }
        }
        return result;
    }

    @Override
    public KeyValue read(String serviceId, String key) {
        log.info("Reading record for service {} under key {}", serviceId, key);
        KeyValue serviceCache = getCache().get(serviceId + key);
        if (serviceCache != null) {
            return serviceCache;
        } else {
            throw new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), key, serviceId);
        }
    }

    @Override
    public KeyValue update(String serviceId, KeyValue toUpdate) {
        toUpdate.setServiceId(serviceId);
        log.info("Updating record for service {} under key {}", serviceId, toUpdate.getKey());
        KeyValue serviceCache = getCache().put(serviceId + toUpdate.getKey(), toUpdate);
        if (serviceCache == null) {
            throw new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), toUpdate.getKey(), serviceId);
        }
        return toUpdate;

    }

    @Override
    public KeyValue delete(String serviceId, String toDelete) {
        log.info("Removing record for service {} under key {}", serviceId, toDelete);
        KeyValue entry = getCache().remove(serviceId + toDelete);
        if (entry != null) {
            return entry;
        } else {
            throw new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), toDelete, serviceId);
        }
    }

    @Override
    public Map<String, KeyValue> readForService(String serviceId) {
        log.info("Reading all records for service {} ", serviceId);
        Map<String, KeyValue> result = new HashMap<>();
        getCache().forEach((key, value) -> {
            if (serviceId.equals(value.getServiceId())) {
                result.put(value.getKey(), value);
            }
        });
        return result;
    }

    @Override
    public void deleteForService(String serviceId) {
        log.info("Removing all records for service {} ", serviceId);
        getCache().forEach((key, value) -> {
            if (value.getServiceId().equals(serviceId)) {
                getCache().remove(key);
            }
        });
    }

    @Override
    public void removeNonRelevantTokens(String serviceId, String mapKey) {
        removeNonRelevant(serviceId, mapKey, this::isExpiredToken);
    }

    @Override
    public void removeNonRelevantRules(String serviceId, String mapKey) {
        long now = System.currentTimeMillis();
        removeNonRelevant(serviceId, mapKey, value -> isExpiredRule(value, now));
    }

    private void removeNonRelevant(String serviceId, String mapKey, Predicate<String> nonRelevant) {
        String prefix = encodeMapPrefix(serviceId, mapKey);
        Cache<String, String> cache = getTokenItemCache();

        int inspected = 0;
        int removed = 0;
        for (String key : cache.keySet()) {
            if (!key.startsWith(prefix)) continue;
            String value = cache.get(key);
            if (value == null) continue;
            inspected++;
            if (nonRelevant.test(value) && cache.remove(key, value)) {
                removed++;
            }
        }
        log.debug("Evicted {} of {} items of map {} for service {}", removed, inspected, mapKey, serviceId);
    }

    private boolean isExpiredToken(String value) {
        try {
            AccessTokenContainer container = objectMapper.readValue(value, AccessTokenContainer.class);
            if (container == null || container.getExpiresAt() == null) {
                return false;
            }
            return container.getExpiresAt().atZone(ZoneId.systemDefault()).isBefore(ZonedDateTime.now(ZoneId.systemDefault()));
        } catch (JsonProcessingException e) {
            log.debug("Cannot parse an invalidated record, keeping it", e);
            return false;
        }
    }

    private boolean isExpiredRule(String value, long now) {
        try {
            long delta = now - Long.parseLong(value.trim());
            return TimeUnit.MILLISECONDS.toDays(delta) > RULE_RETENTION_DAYS;
        } catch (NumberFormatException e) {
            log.debug("Cannot parse a revocation rule timestamp, keeping the rule", e);
            return false;
        }
    }

    /**
     * @return the lifespan to store the entry with, in seconds; {@link #NO_EXPIRY} for an entry that is
     * stored without expiration, and zero or less for one already past its retention, which must be
     * removed rather than stored.
     */
    private long resolveTtlSeconds(String mapKey, KeyValue toCreate) {
        Long requested = toCreate.getTtlSeconds();
        if (requested != null) {
            log.debug("Requested ttl seconds {} for map key {}", requested, mapKey);
            return Math.min(requested, maxTtlSeconds);
        }

        if (!isRevocationMapKey(mapKey)) {
            log.debug("An arbitrary map, written through the public cache-list API. This entry will never expire.");
            return NO_EXPIRY;
        }

        Long derived = deriveTtlSeconds(mapKey, toCreate.getValue());
        if (derived == null) {
            log.debug("A revocation record whose own expiry this service cannot read. Falling back to the maximum retention {}", maxTtlSeconds);
            return maxTtlSeconds;
        }
        return Math.min(derived, maxTtlSeconds);
    }

    static boolean isRevocationMapKey(String mapKey) {
        return INVALID_TOKENS_KEY.equals(mapKey)
            || INVALID_USERS_KEY.equals(mapKey)
            || INVALID_SCOPES_KEY.equals(mapKey);
    }

    private Long deriveTtlSeconds(String mapKey, String value) {
        if (value == null) {
            return null;
        }
        try {
            if (INVALID_TOKENS_KEY.equals(mapKey)) {
                AccessTokenContainer container = objectMapper.readValue(value, AccessTokenContainer.class);
                if (container == null || container.getExpiresAt() == null) {
                    return null;
                }
                return Duration.between(ZonedDateTime.now(ZoneId.systemDefault()), container.getExpiresAt().atZone(ZoneId.systemDefault())).getSeconds();
            }
            if (INVALID_USERS_KEY.equals(mapKey) || INVALID_SCOPES_KEY.equals(mapKey)) {
                long relevantUntil = Long.parseLong(value.trim()) + Duration.ofDays(RULE_RETENTION_DAYS).toMillis();
                return Duration.ofMillis(relevantUntil - System.currentTimeMillis()).getSeconds();
            }
        } catch (JsonProcessingException | NumberFormatException | ArithmeticException e) {
            log.debug("Cannot derive the expiration of an item of map {}", mapKey);
        }
        return null;
    }

    /**
     * {@code len(serviceId)|serviceId + len(mapKey)|mapKey + itemKey}.
     * <p>
     * Length prefixes rather than a separator character: {@code serviceId} is a certificate subject DN,
     * {@code mapKey} is a URL path variable and {@code itemKey} is a hash, so no character is guaranteed to
     * be absent from all three. Prefixing also removes the ambiguity the previous bare concatenation had,
     * where one service id being a prefix of another leaked entries across services.
     */
    static String encodeItemKey(String serviceId, String mapKey, String itemKey) {
        return encodeMapPrefix(serviceId, mapKey) + itemKey;
    }

    static String encodeMapPrefix(String serviceId, String mapKey) {
        return encodeServicePrefix(serviceId) + mapKey.length() + "|" + mapKey;
    }

    static String encodeServicePrefix(String serviceId) {
        return serviceId.length() + "|" + serviceId;
    }

    /**
     * @param offset index just past the service prefix
     * @return {@code [mapKey, itemKey]}, or null when the remainder is not a valid length-prefixed string
     */
    static String[] decodeAfterService(String key, int offset) {
        int separator = key.indexOf('|', offset);
        if (separator < 0) {
            return null;
        }
        int length;
        try {
            length = Integer.parseInt(key.substring(offset, separator));
        } catch (NumberFormatException e) {
            return null;
        }
        int start = separator + 1;
        int end = start + length;
        if (length < 0 || end > key.length()) {
            return null;
        }
        return new String[]{key.substring(start, end), key.substring(end)};
    }

    private void warnIfStoreTooLarge(Cache<String, String> cache) {
        if (writeCounter.incrementAndGet() % DEFAULT_SIZE_CHECK_INTERVAL != 0) {
            return;
        }

        long size;
        try {
            size = cache.size();
        } catch (RuntimeException e) {
            log.debug("Cannot determine the size of the revocation store", e);
            return;
        }
        lastObservedSize.set(size);

        if (sizeWarningThreshold <= 0 || size < sizeWarningThreshold) {
            return;
        }
        long previous = lastWarnedSize.get();
        if (previous != 0 && size < previous * 2) {
            return;
        }
        if (lastWarnedSize.compareAndSet(previous, size)) {
            apimlLog.log("org.zowe.apiml.cache.revocationStoreTooLarge", size, sizeWarningThreshold);
        }
    }

    /**
     * Size of the revocation store as of the last sample, or -1 before the first one.
     */
    public long getLastObservedRevocationStoreSize() {
        return lastObservedSize.get();
    }

}
