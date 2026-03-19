/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.eureka.web;

import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Applications;
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
class EurekaRegistryVersionEndpointTest {

    @Mock
    private EurekaClient eurekaClient;

    private EurekaRegistryVersionEndpoint eurekaRegistryVersionEndpoint;

    @BeforeEach
    public void setup() {
        eurekaRegistryVersionEndpoint = new EurekaRegistryVersionEndpoint(eurekaClient);
    }

    @ParameterizedTest(name = "When eurekaClient.getApplications().getAppsHashCode() is {0} then expected /eurekaversion is {1}")
    @MethodSource("provideTestData")
    void getCorrectVersionOnEurekaEvent(String hashcode, Long expectedVersion) {
        Mockito.when(eurekaClient.getApplications()).thenReturn(Mockito.mock(Applications.class));
        Mockito.when(eurekaClient.getApplications().getAppsHashCode()).thenReturn(hashcode);

        eurekaRegistryVersionEndpoint.onRegistryUpdate(null);

        assertEquals(EurekaRegistryVersionEndpoint.VersionDto.builder().version(expectedVersion).build(), eurekaRegistryVersionEndpoint.status());
    }

    static Stream<Arguments> provideTestData() {
        return Stream.of(
            Arguments.of("DOWN_12_UP_3_", 3L),
            Arguments.of("DOWN_14_", -1L),
            Arguments.of("UP_24_", 24L));
    }

}
