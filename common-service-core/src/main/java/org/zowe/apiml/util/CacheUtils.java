/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCache;
import org.zowe.apiml.cache.CompositeKey;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * This utils offer base operation with cache, which can be shared to multiple codes.
 */
public class CacheUtils {

    /**
     * This method evict a part of cache by condition on key if method use composite keys. It cannot be used on cache
     * with just one parameter.
     *
     * Method iterate over all keys in cache and evict just a part (if keyPredicate return true). It requires to use
     * ehCache and {@link CompositeKey}. If there is no way how to filter the entries in the cache, it will remove all
     * of them (different provider than EhCache, different type of keys).
     *
     * If entry is not stored as CompositeKey (ie. just one argumentm different keyGenerator) it will be removed allways,
     * without any test. If cache contains other entries witch CompositeKey, they will be checked and removed only in
     * case keyPredicate match.
     *
     * Example:
     *
     * <pre>
     * @Cacheable(value = "<cacheName>", keyGenerator = CacheConfig.COMPOSITE_KEY_GENERATOR)
     * public <ReturnType> cacheSomething(arg1, arg2, ...) {
     *     // do something
     *     return <value to cache>;
     * }
     *
     * @Autowired CacheManager cacheManager;
     *
     * public void evictByArg1(arg1) {
     *     CacheUtils.evictSubset(cacheManager, "<cacheName>", x -> x.equals(0, arg1));
     *     // alternatively CacheUtils.evictSubset(cacheManager, "<cacheName>", x -> ObjectUtils.equals(x.get(0), arg1));
     * }
     * </pre>
     *
     * @param cacheManager manager collecting the cache
     * @param cacheName name of cache
     * @param keyPredicate condition to filter keys to evict
     */
    public void evictSubset(CacheManager cacheManager, String cacheName, Predicate<CompositeKey> keyPredicate) {
        final Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) throw new IllegalArgumentException("Unknown cache " + cacheName);
        final Object nativeCache = cache.getNativeCache();
        if (nativeCache instanceof javax.cache.Cache) {
            Spliterator<javax.cache.Cache.Entry<Object, Object>> spliterator = ((javax.cache.Cache<Object, Object>) nativeCache).spliterator();
            Set<Object> keysToRemove = StreamSupport.stream(spliterator, true)
                    // if the key matches the predicate then evict the record or
                    // if the key is not compositeKey (unknown for evict) evict record (as failover)
                .filter(e -> !(e.getKey() instanceof CompositeKey key) || keyPredicate.test(key))
                    .map(javax.cache.Cache.Entry::getKey)
                    .collect(Collectors.toSet());
            ((javax.cache.Cache<Object, Object>) nativeCache).removeAll(keysToRemove);
        } else {
            // in case of using different cache manager, evict all records for sure
            cache.clear();
        }
    }

    /**
     * This method read all stored records in the cache. It supports only EhCache, for other cache managers it throws
     * an exception.
     *
     * @param cacheManager manager collecting the cache
     * @param cacheName name of cache
     * @param <T> type of stored elements
     * @return collection with all stored records
     */
    public <T> List<T> getAllRecords(CacheManager cacheManager, String cacheName) {
        final Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) throw new IllegalArgumentException("Unknown cache " + cacheName);

        final Object nativeCache = cache.getNativeCache();
        if (nativeCache instanceof javax.cache.Cache) {
            Spliterator<javax.cache.Cache.Entry<Object, T>> spliterator = ((javax.cache.Cache<Object, T>) nativeCache).spliterator();
            return StreamSupport.stream(spliterator, true).map(javax.cache.Cache.Entry::getValue).toList();
        } else if (nativeCache instanceof NoOpCache) {
            return Collections.emptyList();
        } else {
            throw new IllegalArgumentException("Unsupported type of cache : " + nativeCache.getClass());
        }
    }

}
