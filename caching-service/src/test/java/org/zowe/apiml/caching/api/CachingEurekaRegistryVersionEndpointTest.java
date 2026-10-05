/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zowe.apiml.eurekaservice.client.ApiMediationClient;
import org.zowe.apiml.registry.client.RegistryCache;
import org.zowe.apiml.registry.client.RegistryClient;
import org.zowe.apiml.registry.model.Applications;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class CachingEurekaRegistryVersionEndpointTest {

    @Mock
    private ApiMediationClient apiMediationClient;

    private CachingEurekaRegistryVersionEndpoint cachingEurekaRegistryVersionEndpoint;

    @BeforeEach
    public void setup() {
        cachingEurekaRegistryVersionEndpoint = new CachingEurekaRegistryVersionEndpoint(apiMediationClient);
    }

    @ParameterizedTest(name = "When the cached registry hash code is {0} then expected /eurekaversion is {1}")
    @MethodSource("provideTestData")
    void getCorrectVersionOnEurekaEvent(boolean hasClient, String hashcode, Long expectedVersion) {
        if (hasClient) {
            var registryClient = Mockito.mock(RegistryClient.class);
            var cache = Mockito.mock(RegistryCache.class);
            Mockito.doReturn(cache).when(registryClient).cache();
            Mockito.doReturn(new Applications(List.of(), 0L, hashcode)).when(cache).applications();
            Mockito.when(apiMediationClient.getRegistryClient()).thenReturn(registryClient);
        } else {
            Mockito.when(apiMediationClient.getRegistryClient()).thenReturn(null);
        }

        assertEquals(CachingEurekaRegistryVersionEndpoint.VersionDto.builder().version(expectedVersion).build(), cachingEurekaRegistryVersionEndpoint.status());
    }

    static Stream<Arguments> provideTestData() {
        return Stream.of(
            Arguments.of(false, "DOWN_12_UP_3_", -1L),
            Arguments.of(true, "DOWN_12_UP_3_", 3L),
            Arguments.of(true, "DOWN_14_", -1L),
            Arguments.of(true, "UP_24_", 24L));
    }


}
