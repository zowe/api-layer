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

import org.zowe.apiml.cache.CompositeKey;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.function.Predicate;

/**
 * This utils offer base operation with cache, which can be shared to multiple codes.
 */
public final class CacheUtils {

    private CacheUtils() {}

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
    public static void evictSubset(CacheManager cacheManager, String cacheName, Predicate<CompositeKey> keyPredicate) {
        final Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) throw new IllegalArgumentException("Unknown cache " + cacheName);
        final Object nativeCache = cache.getNativeCache();
        if (nativeCache instanceof net.sf.ehcache.Cache) {
            final net.sf.ehcache.Cache ehCache = (net.sf.ehcache.Cache) nativeCache;

            for (final Object key : ehCache.getKeys()) {
                if (key instanceof CompositeKey) {
                    // if entry is compositeKey and first param is different, skip it (be sure this is not to evict)
                    final CompositeKey compositeKey = ((CompositeKey) key);
                    if (!keyPredicate.test(compositeKey)) continue;
                }
                // if key is not composite key (unknown for evict) or has same serviceId, evict record
                ehCache.remove(key);
            }
        } else {
            // in case of using different cache manager, evict all records for sure
            cache.clear();
        }
    }

}
