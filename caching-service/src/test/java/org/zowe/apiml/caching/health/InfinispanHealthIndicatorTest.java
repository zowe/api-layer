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
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;
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
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.caching.service.infinispan.config.LazyCacheManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class InfinispanHealthIndicatorTest {

    @Nested
    class BeforeSpringStartup {

        @Test
        void givenApplication_whenStarting_thenReturnUnknownStatus() {
            var infinispanHealthIndicator = new InfinispanHealthIndicator();
            var builder = mock(Health.Builder.class);
            infinispanHealthIndicator.doHealthCheck(builder);
            verify(builder).unknown();
        }

    }

    @Nested
    class UninitializedCacheManager {

        @Test
        void repeatedHealthProbesDoNotInitializeCaches() {
            var nativeCacheManager = mock(LazyCacheManager.class);
            var cacheManager = mock(SpringEmbeddedCacheManager.class);
            when(cacheManager.getNativeCacheManager()).thenReturn(nativeCacheManager);
            var indicator = new InfinispanHealthIndicator();
            ((AtomicReference<CacheManager>) ReflectionTestUtils.getField(indicator, "cacheManager")).set(cacheManager);

            for (int i = 0; i < 3; i++) {
                var health = indicator.health();
                assertEquals(Status.OUT_OF_SERVICE, health.getStatus());
                assertEquals(Map.of("initialized", false), health.getDetails().get("infinispan"));
            }

            verify(nativeCacheManager, times(3)).isInitialized();
            verify(cacheManager, times(3)).getNativeCacheManager();
            verifyNoMoreInteractions(nativeCacheManager, cacheManager);
        }

    }

    @Nested
    class UnsupportedCacheManager {

        @Test
        void givenUnsupportedCacheManager_whenBuildHealth_thenNoDetailsAdded() {
            var infinispanHealthIndicator = new InfinispanHealthIndicator();
            ((AtomicReference<CacheManager>) ReflectionTestUtils.getField(infinispanHealthIndicator, "cacheManager"))
                .set(mock(CacheManager.class));
            var builder = mock(Health.Builder.class);
            infinispanHealthIndicator.doHealthCheck(builder);
            verify(builder, never()).withDetail(any(), any());
        }

    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class SupportedCacheManager {

        private static final String CACHES = "caches";
        private static final String INFINISPAN = "infinispan";
        private static final String STATUS = "status";
        private static final String CLUSTER = "cluster";
        private static final String CACHE_1 = "cache_1";

        private SpringEmbeddedCacheManager cacheManager = mock(SpringEmbeddedCacheManager.class);
        private EmbeddedCacheManager nativeCacheManager = mock(EmbeddedCacheManager.class);
        private Cache cache = mock(Cache.class);
        private Address address = mock(Address.class);
        private List<Address> members = Collections.singletonList(address);

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
            doReturn(address).when(nativeCacheManager).getAddress();
            doReturn(members).when(nativeCacheManager).getMembers();
            doReturn(GlobalConfigurationBuilder.defaultClusteredBuilder().build()).when(nativeCacheManager).getCacheManagerConfiguration();
            doReturn(cache).when(nativeCacheManager).getCache(CACHE_1);
            doReturn(cacheStatus).when(cache).getStatus();

            var infinispanHealthIndicator = new InfinispanHealthIndicator();
            ((AtomicReference<CacheManager>) ReflectionTestUtils.getField(infinispanHealthIndicator, "cacheManager"))
                .set(cacheManager);
            System.setProperty("jgroups.tcpping.initial_hosts", "");
            var builder = mock(Health.Builder.class);
            infinispanHealthIndicator.doHealthCheck(builder);

            verify(builder).withDetail(eq(INFINISPAN), mapCaptor.capture());
            var mapDetails = mapCaptor.getValue();
            assertEquals(4, mapDetails.size());
            assertEquals("clustered", mapDetails.get("mode"));
            assertTrue(mapDetails.containsKey(CACHES));
            assertTrue(mapDetails.containsKey(STATUS));
            assertTrue(mapDetails.containsKey(CLUSTER));
            var caches = (Map) mapDetails.get(CACHES);
            assertEquals(cacheStatus, caches.get(CACHE_1));
            assertEquals(wholeStatus, mapDetails.get(STATUS));
            verify(builder).status(result ? Status.UP : Status.DOWN);
        }

        @ParameterizedTest
        @CsvSource({"RUNNING,RUNNING,true", "FAILED,RUNNING,false", "RUNNING,FAILED,false"})
        void givenStandaloneManager_whenBuildHealth_thenOnlyCheckLocalCaches(
            ComponentStatus wholeStatus, ComponentStatus cacheStatus, boolean healthy
        ) {
            doReturn(new GlobalConfigurationBuilder().build()).when(nativeCacheManager).getCacheManagerConfiguration();
            doReturn(wholeStatus).when(nativeCacheManager).getStatus();
            doReturn(List.of(CACHE_1)).when(cacheManager).getCacheNames();
            doReturn(cache).when(nativeCacheManager).getCache(CACHE_1);
            doReturn(cacheStatus).when(cache).getStatus();
            var indicator = new InfinispanHealthIndicator();
            ((AtomicReference<CacheManager>) ReflectionTestUtils.getField(indicator, "cacheManager")).set(cacheManager);
            var builder = new Health.Builder();

            indicator.doHealthCheck(builder);

            var health = builder.build();
            assertEquals(healthy ? Status.UP : Status.DOWN, health.getStatus());
            var details = (Map<?, ?>) health.getDetails().get(INFINISPAN);
            assertEquals("standalone", details.get("mode"));
            assertFalse(details.containsKey(CLUSTER));
            verify(nativeCacheManager, never()).getMembers();
            verify(nativeCacheManager, never()).getAddress();
        }

    }

}
