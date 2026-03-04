/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import com.netflix.discovery.shared.Applications;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ApimlEurekaRegistryVersionEndpointTest {

    @Mock
    private PeerAwareInstanceRegistry peerAwareInstanceRegistry;

    private ApimlEurekaRegistryVersionEndpoint apimlEurekaRegistryVersionEndpoint;

    @BeforeEach
    public void setup() {
        apimlEurekaRegistryVersionEndpoint = new ApimlEurekaRegistryVersionEndpoint(peerAwareInstanceRegistry);
    }

    @ParameterizedTest(name = "When peerAwareInstanceRegistry.getApplications().getAppsHashCode() is {0} then expected /eurekaversion is {1}")
    @MethodSource("provideTestData")
    void getCorrectVersionOnEurekaEvent(String hashcode, Long expectedVersion) {
        Mockito.when(peerAwareInstanceRegistry.getApplications()).thenReturn(Mockito.mock(Applications.class));
        Mockito.when(peerAwareInstanceRegistry.getApplications().getAppsHashCode()).thenReturn(hashcode);

        assertEquals(ApimlEurekaRegistryVersionEndpoint.VersionDto.builder().version(expectedVersion).build(), apimlEurekaRegistryVersionEndpoint.status());
    }

    static Stream<Arguments> provideTestData() {
        return Stream.of(
            Arguments.of("DOWN_12_UP_3_", 3L),
            Arguments.of("DOWN_14_", -1L),
            Arguments.of("UP_24_", 24L));
    }

}
