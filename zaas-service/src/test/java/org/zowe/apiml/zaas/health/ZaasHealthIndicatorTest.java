/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.zowe.apiml.zaas.security.login.Providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ZaasHealthIndicatorTest {

    private Providers providers;

    @BeforeEach
    void setUp() {
        providers = mock(Providers.class);
    }

    @Nested
    class GivenZosmfIsUsedAnfZosmfIsUnavailable {
        @Test
        void whenHealthIsRequested_thenStatusIsDown() {
            when(providers.isZosfmUsed()).thenReturn(true);
            when(providers.isZosmfAvailableAndOnline()).thenReturn(false);

            DiscoveryClient discoveryClient = mock(DiscoveryClient.class);

            ZaasHealthIndicator zaasHealthIndicator = new ZaasHealthIndicator(discoveryClient, providers);
            Health.Builder builder = new Health.Builder();
            zaasHealthIndicator.doHealthCheck(builder);
            assertEquals(Status.DOWN, builder.build().getStatus());
        }
    }

    @Nested
    class GivenZosmfIsUsedAndAvailable {
        @Test
        void whenHealthIsRequested_thenStatusIsUp() {
            when(providers.isZosfmUsed()).thenReturn(true);
            when(providers.isZosmfAvailableAndOnline()).thenReturn(true);

            DiscoveryClient discoveryClient = mock(DiscoveryClient.class);

            ZaasHealthIndicator zaasHealthIndicator = new ZaasHealthIndicator(discoveryClient, providers);
            Health.Builder builder = new Health.Builder();
            zaasHealthIndicator.doHealthCheck(builder);
            assertEquals(Status.UP, builder.build().getStatus());
        }
    }
}
