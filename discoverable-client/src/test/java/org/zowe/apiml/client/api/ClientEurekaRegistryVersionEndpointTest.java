/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.client.api;

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
import org.zowe.apiml.eurekaservice.client.ApiMediationClient;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ClientEurekaRegistryVersionEndpointTest {

    @Mock
    private ApiMediationClient apiMediationClient;

    private ClientEurekaRegistryVersionEndpoint clientEurekaRegistryVersionEndpoint;

    @BeforeEach
    public void setup() {
        clientEurekaRegistryVersionEndpoint = new ClientEurekaRegistryVersionEndpoint(apiMediationClient);
    }

    @ParameterizedTest(name = "When eurekaClient.getApplications().getAppsHashCode() is {0} then expected /eurekaversion is {1}")
    @MethodSource("provideTestData")
    void getCorrectVersionOnEurekaEvent(EurekaClient eurekaClient, String hashcode, Long expectedVersion) {
        Mockito.when(apiMediationClient.getEurekaClient()).thenReturn(eurekaClient);
        if (eurekaClient != null) {
            Mockito.when(eurekaClient.getApplications()).thenReturn(Mockito.mock(Applications.class));
            Mockito.when(eurekaClient.getApplications().getAppsHashCode()).thenReturn(hashcode);
        }

        assertEquals(ClientEurekaRegistryVersionEndpoint.VersionDto.builder().version(expectedVersion).build(), clientEurekaRegistryVersionEndpoint.status());
    }

    static Stream<Arguments> provideTestData() {
        EurekaClient eurekaClient = Mockito.mock(EurekaClient.class);
        return Stream.of(
            Arguments.of(null, "DOWN_12_UP_3_", -1L),
            Arguments.of(eurekaClient, "DOWN_12_UP_3_", 3L),
            Arguments.of(eurekaClient, "DOWN_14_", -1L),
            Arguments.of(eurekaClient, "UP_24_", 24L));
    }

}
