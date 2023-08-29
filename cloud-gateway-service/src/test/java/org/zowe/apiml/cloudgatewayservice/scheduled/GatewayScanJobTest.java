/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.scheduled;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.cloudgatewayservice.service.GatewayIndexService;
import org.zowe.apiml.cloudgatewayservice.service.InstanceInfoService;
import org.zowe.apiml.cloudgatewayservice.service.ServiceInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.cloudgatewayservice.scheduled.GatewayScanJob.GATEWAY_SERVICE_ID;


@ExtendWith(MockitoExtension.class)
class GatewayScanJobTest {

    @Mock
    private ServiceInstance instanceOne;
    @Mock
    private ServiceInstance instanceTwo;
    @Mock
    private List<ServiceInfo> apimlServicesOne;
    @Mock
    private List<ServiceInfo> apimlServicesTwo;
    @Mock
    private GatewayIndexService gatewayIndexerService;
    @Mock
    private InstanceInfoService instanceInfoService;
    @InjectMocks
    private GatewayScanJob gatewayScanJob;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(gatewayScanJob, "parallelismLevel", 3);

        lenient().when(instanceInfoService.getServiceInstance(GATEWAY_SERVICE_ID)).thenReturn(Mono.just(asList(instanceOne, instanceTwo)));

        lenient().when(gatewayIndexerService.indexGatewayServices(instanceOne)).thenReturn(Mono.just(apimlServicesOne));
        lenient().when(gatewayIndexerService.indexGatewayServices(instanceTwo)).thenReturn(Mono.just(apimlServicesTwo));
    }

    @Nested
    class WhenScanningExternalGateway {
        @Test
        void shouldTriggerIndexingForRegisteredGateways() {
            StepVerifier.create(gatewayScanJob.doScanExternalGateway())
                    .expectNext(apimlServicesOne)
                    .expectNext(apimlServicesTwo)
                    .verifyComplete();

            verify(gatewayIndexerService).indexGatewayServices(instanceOne);
            verify(gatewayIndexerService).indexGatewayServices(instanceTwo);
            verifyNoMoreInteractions(gatewayIndexerService);
        }
    }

    @Test
    void scheduledCallShouldTriggerReactiveAction() {
        GatewayScanJob spy = spy(gatewayScanJob);
        when(spy.doScanExternalGateway()).thenReturn(Flux.empty());

        spy.startScanExternalGatewayJob();

        verify(spy).doScanExternalGateway();
    }

    @Test
    void shouldListRegistry() {
        ServiceInfo serviceInfo = mock(ServiceInfo.class);
        when(gatewayIndexerService.listRegistry(null, null)).thenReturn(singletonMap("testApimlId", singletonList(serviceInfo)));
        when(gatewayIndexerService.listRegistry("testApimlId", null)).thenReturn(singletonMap("testApimlId", singletonList(serviceInfo)));
        when(gatewayIndexerService.listRegistry(null, "bcm.sysview")).thenReturn(singletonMap("testApimlId", singletonList(serviceInfo)));

        gatewayScanJob.listCaches();

        verify(gatewayIndexerService).listRegistry(null, null);
        verify(gatewayIndexerService).listRegistry("testApimlId", null);
        verify(gatewayIndexerService).listRegistry(null, "bcm.sysview");
    }
}
