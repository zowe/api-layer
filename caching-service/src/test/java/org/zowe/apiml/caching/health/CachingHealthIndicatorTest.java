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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.zowe.apiml.eurekaservice.client.ApiMediationClient;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.registry.client.RegistryClient;
import org.zowe.apiml.registry.client.RegistryCache;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CachingHealthIndicatorTest {

    @Mock
    private ApiMediationClient apiMediationClient;

    @Mock
    private Health.Builder builder;

    void initRegistry(boolean hasGw, boolean hasGwInstance) {
        var registryClient = mock(RegistryClient.class);
        doReturn(registryClient).when(apiMediationClient).getRegistryClient();
        var cache = mock(RegistryCache.class);
        doReturn(cache).when(registryClient).cache();
        doReturn(hasGw && hasGwInstance ? List.of(mock(ServiceInstance.class)) : List.<ServiceInstance>of())
            .when(cache).instances(CoreService.GATEWAY.getServiceId());
    }

    @Nested
    class WithoutCacheIndicator {

        @Test
        void givenNoRegistryClient_whenBuildHealthIndicator_thenItIsDown() {
            new CachingHealthIndicator(apiMediationClient, Optional.empty()).doHealthCheck(builder);
            verify(builder).withDetail(CoreService.GATEWAY.getServiceId(), Status.DOWN);
            verify(builder).down();
        }

        @Test
        void givenNoService_whenBuildHealthIndicator_thenItIsDown() {
            initRegistry(false, false);
            new CachingHealthIndicator(apiMediationClient, Optional.empty()).doHealthCheck(builder);
            verify(builder).withDetail(CoreService.GATEWAY.getServiceId(), Status.DOWN);
            verify(builder).down();
        }

        @Test
        void givenNoGwInstance_whenBuildHealthIndicator_thenItIsDown() {
            initRegistry(true, false);
            new CachingHealthIndicator(apiMediationClient, Optional.empty()).doHealthCheck(builder);
            verify(builder).withDetail(CoreService.GATEWAY.getServiceId(), Status.DOWN);
            verify(builder).down();
        }

        @Test
        void givenGatewayInstanceBeforeStartUp_whenBuildHealthIndicator_thenItIsDown() {
            initRegistry(true, true);
            new CachingHealthIndicator(apiMediationClient, Optional.empty()).doHealthCheck(builder);
            verify(builder).withDetail(CoreService.GATEWAY.getServiceId(), Status.UP);
            verify(builder).down();
        }

        @Test
        void givenGatewayInstanceAfterStartUp_whenBuildHealthIndicator_thenItIsUp() {
            initRegistry(true, true);
            var cachingHealthIndicator = new CachingHealthIndicator(apiMediationClient, Optional.empty());
            cachingHealthIndicator.onApplicationEvent(mock(ApplicationReadyEvent.class));
            cachingHealthIndicator.doHealthCheck(builder);
            verify(builder).withDetail(CoreService.GATEWAY.getServiceId(), Status.UP);
            verify(builder, never()).down();
        }

    }

    @Nested
    class WithCacheIndicator {

        @Mock
        private InfinispanHealthIndicator infinispanHealthIndicator;

        @Test
        void givenNoGateway_whenBuildHealthIndicator_thenItIsDown() {
            initRegistry(false, false);
            var cachingHealthIndicator = new CachingHealthIndicator(apiMediationClient, Optional.of(infinispanHealthIndicator));
            cachingHealthIndicator.onApplicationEvent(mock(ApplicationReadyEvent.class));
            cachingHealthIndicator.doHealthCheck(builder);
            verify(infinispanHealthIndicator).doHealthCheck(builder);
            verify(builder).down();
        }

        @Test
        void givenNoStartUpEvent_whenBuildHealthIndicator_thenItIsDown() {
            initRegistry(true, true);
            var cachingHealthIndicator = new CachingHealthIndicator(apiMediationClient, Optional.of(infinispanHealthIndicator));
            cachingHealthIndicator.doHealthCheck(builder);
            verify(infinispanHealthIndicator).doHealthCheck(builder);
            verify(builder).down();
        }

        @Test
        void givenEverythingReady_whenBuildHealthIndicator_thenItIsUp() {
            initRegistry(true, true);
            var cachingHealthIndicator = new CachingHealthIndicator(apiMediationClient, Optional.of(infinispanHealthIndicator));
            cachingHealthIndicator.onApplicationEvent(mock(ApplicationReadyEvent.class));
            cachingHealthIndicator.doHealthCheck(builder);
            verify(infinispanHealthIndicator).doHealthCheck(builder);
            verify(builder, never()).down();
        }

    }

}
