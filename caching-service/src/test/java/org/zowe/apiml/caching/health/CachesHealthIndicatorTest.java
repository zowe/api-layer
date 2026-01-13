/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.health;

import org.infinispan.Cache;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.spring.embedded.provider.SpringEmbeddedCacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cache.CacheManager;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class CachesHealthIndicatorTest {

    @Nested
    class UnsupportedCacheManager {

        @Test
        void givenUnsupportedCacheManager_whenBuildHealth_thenNoDetailsAdded() {
            var cachesHealthIndicator = new CachesHealthIndicator(mock(CacheManager.class));
            var builder = mock(Health.Builder.class);
            cachesHealthIndicator.doHealthCheck(builder);
            verify(builder, never()).withDetail(any(), any());
        }

    }


    @Nested
    @ExtendWith(MockitoExtension.class)
    class SupportedCacheManager {

        private static final String CACHES = "caches";
        private static final String INFINISPAN = "infinispan";
        private static final String STATUS = "status";
        private static final String CACHE_1 = "cache_1";

        private SpringEmbeddedCacheManager cacheManager = mock(SpringEmbeddedCacheManager.class);
        private EmbeddedCacheManager nativeCacheManager = mock(EmbeddedCacheManager.class);
        private Cache cache = mock(Cache.class);

        @Captor
        private ArgumentCaptor<Map> mapCaptor;

        @BeforeEach
        void setUp() {
            doReturn(nativeCacheManager).when(cacheManager).getNativeCacheManager();
        }

        @ParameterizedTest
        @CsvSource({
            "FAILED,RUNNING,false",
            "FAILED,FAILED,false",
            "RUNNING,FAILED,false",
            "RUNNING,RUNNING,true"
        })
        void givenNonRunningCache_whenBuildHealth_thenItIsDown(
            ComponentStatus wholeStatus,
            ComponentStatus cacheStatus,
            boolean result
        ) {
            doReturn(Arrays.asList(CACHE_1)).when(cacheManager).getCacheNames();
            doReturn(wholeStatus).when(nativeCacheManager).getStatus();
            doReturn(cache).when(nativeCacheManager).getCache(CACHE_1);
            doReturn(cacheStatus).when(cache).getStatus();

            var cachesHealthIndicator = new CachesHealthIndicator(cacheManager);
            var builder = mock(Health.Builder.class);
            cachesHealthIndicator.doHealthCheck(builder);

            verify(builder).withDetail(eq(INFINISPAN), mapCaptor.capture());
            var mapDetails = mapCaptor.getValue();
            assertEquals(2, mapDetails.size());
            assertTrue(mapDetails.containsKey(CACHES));
            assertTrue(mapDetails.containsKey(STATUS));
            var caches = (Map) mapDetails.get(CACHES);
            assertEquals(cacheStatus, caches.get(CACHE_1));
            assertEquals(wholeStatus, mapDetails.get(STATUS));
            verify(builder).status(result ? Status.UP : Status.DOWN);
        }

    }

}
