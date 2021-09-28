/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.routing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RouteUtilTest {
    private static final String SERVICE_ID = "serviceid";
    private DiscoveryClient discoveryClient;

    @BeforeEach
    void setup() {
        discoveryClient = mock(DiscoveryClient.class);
    }

    @Nested
    class WhenGetInstanceInfoForUri {
        @Nested
        class ThenReturnEmpty {
            @Test
            void givenUriWithNoPath() {
                Optional<ServiceInstance> result = RouteUtil.getInstanceInfoForUri("", discoveryClient);
                assertThat(result, is(Optional.empty()));
            }

            @Nested
            class GivenOldPathFormat {
                @Test
                void whenNoServiceIdWithApiUri() {
                    Optional<ServiceInstance> result = RouteUtil.getInstanceInfoForUri("/api/v1", discoveryClient);
                    assertThat(result, is(Optional.empty()));
                }

                @Test
                void whenNoServiceIdWithUiUri() {
                    Optional<ServiceInstance> result = RouteUtil.getInstanceInfoForUri("/ui/v1", discoveryClient);
                    assertThat(result, is(Optional.empty()));
                }

                @Test
                void whenNoInstanceInDiscoveryClient() {
                    when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(Collections.emptyList());

                    Optional<ServiceInstance> result = RouteUtil.getInstanceInfoForUri("/api/v1/" + SERVICE_ID, discoveryClient);
                    assertThat(result, is(Optional.empty()));
                }
            }

            @Nested
            class GivenNewPathFormat {
                @Test
                void whenNullInstancesInDiscovery() {
                    when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(null);

                    Optional<ServiceInstance> result = RouteUtil.getInstanceInfoForUri("/" + SERVICE_ID + "/api/v1", discoveryClient);
                    assertThat(result, is(Optional.empty()));
                }

                @Test
                void whenEmptyInstancesInDiscovery() {
                    when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(Collections.emptyList());

                    Optional<ServiceInstance> result = RouteUtil.getInstanceInfoForUri("/" + SERVICE_ID + "/api/v1", discoveryClient);
                    assertThat(result, is(Optional.empty()));
                }
            }
        }

        @Nested
        class ThenReturnFirstInstance {
            private final ServiceInstance service1 = mock(ServiceInstance.class);
            private final ServiceInstance service2 = mock(ServiceInstance.class);
            private final List<ServiceInstance> serviceInstances = Arrays.asList(service1, service2);

            @Test
            void givenServiceInstancesWithOldPathFormat() {
                when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(serviceInstances);

                Optional<ServiceInstance> result = RouteUtil.getInstanceInfoForUri("/api/v1/" + SERVICE_ID, discoveryClient);
                assertTrue(result.isPresent());
                assertThat(result.get(), is(service1));
            }

            @Test
            void givenServiceInstancesWithNewPathFormat() {
                when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(serviceInstances);

                Optional<ServiceInstance> result = RouteUtil.getInstanceInfoForUri("/" + SERVICE_ID + "/api/v1", discoveryClient);
                assertTrue(result.isPresent());
                assertThat(result.get(), is(service1));
            }
        }
    }
}
