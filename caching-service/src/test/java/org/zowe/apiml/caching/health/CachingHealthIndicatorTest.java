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

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
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

import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CachingHealthIndicatorTest {

    @Mock
    private ApiMediationClient apiMediationClient;

    @Mock
    private Health.Builder builder;

    void initEureka(boolean hasGw, boolean hasGwInstance) {
        EurekaClient eurekaClient = mock(EurekaClient.class);
        doReturn(eurekaClient).when(apiMediationClient).getEurekaClient();
        if (!hasGw) return;

        Application application = mock(Application.class);
        doReturn(application).when(eurekaClient).getApplication(CoreService.GATEWAY.getServiceId());
        if (!hasGwInstance) return;

        doReturn(Collections.singletonList(mock(InstanceInfo.class))).when(application).getInstances();
    }

    @Nested
    class WithoutCacheIndicator {

        @Test
        void givenNoEurekaClient_whenBuildHealthIndicator_thenItIsDown() {
            new CachingHealthIndicator(apiMediationClient, Optional.empty()).doHealthCheck(builder);
            verify(builder).withDetail(CoreService.GATEWAY.getServiceId(), Status.DOWN);
            verify(builder).down();
        }

        @Test
        void givenNoService_whenBuildHealthIndicator_thenItIsDown() {
            initEureka(false, false);
            new CachingHealthIndicator(apiMediationClient, Optional.empty()).doHealthCheck(builder);
            verify(builder).withDetail(CoreService.GATEWAY.getServiceId(), Status.DOWN);
            verify(builder).down();
        }

        @Test
        void givenNoGwInstance_whenBuildHealthIndicator_thenItIsDown() {
            initEureka(true, false);
            new CachingHealthIndicator(apiMediationClient, Optional.empty()).doHealthCheck(builder);
            verify(builder).withDetail(CoreService.GATEWAY.getServiceId(), Status.DOWN);
            verify(builder).down();
        }

        @Test
        void givenGatewayInstanceBeforeStartUp_whenBuildHealthIndicator_thenItIsDown() {
            initEureka(true, true);
            new CachingHealthIndicator(apiMediationClient, Optional.empty()).doHealthCheck(builder);
            verify(builder).withDetail(CoreService.GATEWAY.getServiceId(), Status.UP);
            verify(builder).down();
        }

        @Test
        void givenGatewayInstanceAfterStartUp_whenBuildHealthIndicator_thenItIsUp() {
            initEureka(true, true);
            CachingHealthIndicator cachingHealthIndicator = new CachingHealthIndicator(apiMediationClient, Optional.empty());
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
            initEureka(false, false);
            CachingHealthIndicator cachingHealthIndicator = new CachingHealthIndicator(apiMediationClient, Optional.of(infinispanHealthIndicator));
            cachingHealthIndicator.onApplicationEvent(mock(ApplicationReadyEvent.class));
            cachingHealthIndicator.doHealthCheck(builder);
            verify(infinispanHealthIndicator).doHealthCheck(builder);
            verify(builder).down();
        }

        @Test
        void givenNoStartUpEvent_whenBuildHealthIndicator_thenItIsDown() {
            initEureka(true, true);
            CachingHealthIndicator cachingHealthIndicator = new CachingHealthIndicator(apiMediationClient, Optional.of(infinispanHealthIndicator));
            cachingHealthIndicator.doHealthCheck(builder);
            verify(infinispanHealthIndicator).doHealthCheck(builder);
            verify(builder).down();
        }

        @Test
        void givenEverythingReady_whenBuildHealthIndicator_thenItIsUp() {
            initEureka(true, true);
            CachingHealthIndicator cachingHealthIndicator = new CachingHealthIndicator(apiMediationClient, Optional.of(infinispanHealthIndicator));
            cachingHealthIndicator.onApplicationEvent(mock(ApplicationReadyEvent.class));
            cachingHealthIndicator.doHealthCheck(builder);
            verify(infinispanHealthIndicator).doHealthCheck(builder);
            verify(builder, never()).down();
        }

    }

}
