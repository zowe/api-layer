/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.controller;


import org.apache.groovy.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.cloudgatewayservice.service.CentralApimlInfoMapper;
import org.zowe.apiml.cloudgatewayservice.service.GatewayIndexService;
import org.zowe.apiml.cloudgatewayservice.service.model.ApimlInfo;
import org.zowe.apiml.services.ServiceInfo;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistryControllerTest {
    @Mock
    private GatewayIndexService gatewayIndexService;
    @Mock
    private CentralApimlInfoMapper centralApimlInfoMapper;
    @InjectMocks
    private RegistryController registryController;

    @Nested
    class WhenCentralRegistryIsDisabled {
        @Test
        void shouldReturnEmptyFlux() {

            StepVerifier.create(registryController.getServices(null, null, null))
                    .verifyComplete();

            verifyNoInteractions(centralApimlInfoMapper, gatewayIndexService);
        }
    }

    @Nested
    class WhenCentralRegistryIsEnabled {
        @Mock
        private ApimlInfo apimlInfoOne, apimlInfoTwo;
        @Mock
        private List<ServiceInfo> servicesOne, servicesTwo;

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(registryController, "serviceRegistryEnabled", true);
        }

        @Test
        void shouldFetchTwoApimlInfos() {
            Map<String, List<ServiceInfo>> registryServices = Maps.of("apiml1", servicesOne, "apiml2", servicesTwo);
            when(gatewayIndexService.listRegistry(null, null, null)).thenReturn(registryServices);
            when(centralApimlInfoMapper.buildApimlServiceInfo(any(), any())).thenReturn(apimlInfoOne, apimlInfoTwo);

            StepVerifier.create(registryController.getServices("", null, null))
                    .expectNext(apimlInfoOne)
                    .expectNext(apimlInfoTwo)
                    .verifyComplete();


            verify(gatewayIndexService).listRegistry(null, null, null);
            verify(centralApimlInfoMapper).buildApimlServiceInfo("apiml1", servicesOne);
            verify(centralApimlInfoMapper).buildApimlServiceInfo("apiml2", servicesTwo);
        }

        @Test
        void shouldHandleErrorAndFetchSecondApimlInfo() {
            Map<String, List<ServiceInfo>> registryServices = Maps.of("apiml1", servicesOne, "apiml2", servicesTwo);
            when(gatewayIndexService.listRegistry(null, null, null)).thenReturn(registryServices);
            when(centralApimlInfoMapper.buildApimlServiceInfo(any(), any())).thenReturn(null, apimlInfoTwo);

            StepVerifier.create(registryController.getServices("", "", ""))
                    .expectNext(apimlInfoTwo)
                    .verifyComplete();


            verify(gatewayIndexService).listRegistry(null, null, null);
            verify(centralApimlInfoMapper).buildApimlServiceInfo("apiml1", servicesOne);
            verify(centralApimlInfoMapper).buildApimlServiceInfo("apiml2", servicesTwo);
        }
    }

}
