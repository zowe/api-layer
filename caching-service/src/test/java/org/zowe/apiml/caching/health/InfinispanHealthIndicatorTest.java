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
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.remoting.transport.Address;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class InfinispanHealthIndicatorTest {

    @Nested
    class BeforeSpringStartup {

        @Test
        void givenApplication_whenStarting_thenReturnUnknownStatus() {
            InfinispanHealthIndicator infinispanHealthIndicator = new InfinispanHealthIndicator();
            Health.Builder builder = mock(Health.Builder.class);
            infinispanHealthIndicator.doHealthCheck(builder);
            verify(builder).unknown();
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

        @Mock
        private DefaultCacheManager defaultCacheManager;
        @Mock
        private Cache cache;
        @Mock
        private Address address;

        @Captor
        private ArgumentCaptor<Map<String, Object>> mapCaptor;

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
            List<Address> members = Collections.singletonList(address);

            doReturn(Collections.singleton(CACHE_1)).when(defaultCacheManager).getCacheNames();
            doReturn(wholeStatus).when(defaultCacheManager).getStatus();
            doReturn("nativeAddress").when(address).toString();
            doReturn(address).when(defaultCacheManager).getAddress();
            doReturn(members).when(defaultCacheManager).getMembers();
            doReturn(cache).when(defaultCacheManager).getCache(CACHE_1);
            doReturn(cacheStatus).when(cache).getStatus();

            InfinispanHealthIndicator infinispanHealthIndicator = new InfinispanHealthIndicator();
            ((AtomicReference<DefaultCacheManager>) ReflectionTestUtils.getField(infinispanHealthIndicator, "cacheManager"))
                .set(defaultCacheManager);
            ReflectionTestUtils.setField(infinispanHealthIndicator, "initialHosts", "");
            Health.Builder builder = mock(Health.Builder.class);
            infinispanHealthIndicator.doHealthCheck(builder);

            verify(builder).withDetail(eq(INFINISPAN), mapCaptor.capture());
            Map<String, Object> mapDetails = mapCaptor.getValue();
            assertEquals(3, mapDetails.size());
            assertTrue(mapDetails.containsKey(CACHES));
            assertTrue(mapDetails.containsKey(STATUS));
            assertTrue(mapDetails.containsKey(CLUSTER));
            Map<String, Object> caches = (Map<String, Object>) mapDetails.get(CACHES);
            assertEquals(cacheStatus, caches.get(CACHE_1));
            assertEquals(wholeStatus, mapDetails.get(STATUS));
            verify(builder).status(result ? Status.UP : Status.DOWN);
        }

    }

}
