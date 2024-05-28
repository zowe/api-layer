/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class CacheConfigTest {

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = CacheConfig.class)
    @ActiveProfiles("test")
    class EnabledCache {

        @Autowired
        private CacheManager cacheManager;

        @Test
        void testCacheManagerIsRealImplementation() {
            assertNotNull(cacheManager);
            assertTrue(cacheManager instanceof JCacheCacheManager);
        }

    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = CacheConfig.class, properties = {
        "apiml.caching.enabled=false"
    })
    @ActiveProfiles("test")
    class DisabledCache {


        @Autowired
        private CacheManager cacheManager;

        @Test
        void testDisabledCacheManager() {
            assertNotNull(cacheManager);
            assertTrue(cacheManager instanceof NoOpCacheManager);
        }

    }

}
