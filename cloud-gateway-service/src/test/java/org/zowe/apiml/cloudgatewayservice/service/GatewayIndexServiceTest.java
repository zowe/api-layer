/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.service;


import io.netty.handler.ssl.SslContext;
import org.apache.groovy.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.zowe.apiml.services.ServiceInfo;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.cloudgatewayservice.service.WebClientHelperTest.KEYSTORE_PATH;
import static org.zowe.apiml.cloudgatewayservice.service.WebClientHelperTest.PASSWORD;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.APIML_ID;

@ExtendWith(MockitoExtension.class)
class GatewayIndexServiceTest {
    private GatewayIndexService gatewayIndexService;
    private final ParameterizedTypeReference<List<ServiceInfo>> serviceInfoType = new ParameterizedTypeReference<List<ServiceInfo>>() {
    };
    private ServiceInfo serviceInfoA, serviceInfoB;
    private WebClient webClient;
    private final static String apiCatalogApiId = "zowe.apiml.apicatalog";
    private final static String serviceId = "mockzosmf";
    @Mock
    private ClientResponse clientResponse;
    @Mock
    private ExchangeFunction exchangeFunction;
    @Mock
    private ServiceInstance eurekaInstance;

    @BeforeEach
    void setUp() {

        lenient().when(eurekaInstance.getMetadata()).thenReturn(Maps.of(APIML_ID, "testApimlIdA"));
        lenient().when(eurekaInstance.getInstanceId()).thenReturn("testInstanceIdA");

        serviceInfoA = new ServiceInfo();
        serviceInfoB = new ServiceInfo();

        serviceInfoB.setApiml(new ServiceInfo.Apiml());
        ServiceInfo.ApiInfoExtended sysviewApiInfo = new ServiceInfo.ApiInfoExtended();
        sysviewApiInfo.setApiId(apiCatalogApiId);
        serviceInfoB.setServiceId(serviceId);

        serviceInfoB.getApiml().setApiInfo(Collections.singletonList(sysviewApiInfo));

        webClient = spy(WebClient.builder().exchangeFunction(exchangeFunction).build());
        gatewayIndexService = new GatewayIndexService(webClient, 60, null, null, null);
    }

    @Nested
    class WhenIndexingGatewayService {

        @BeforeEach
        void setUp() {
            lenient().when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(clientResponse));

            lenient().when(clientResponse.bodyToMono(serviceInfoType)).thenReturn(Mono.just(Arrays.asList(serviceInfoA, serviceInfoB)));
        }

        @Test
        void shouldCacheListOfTheServices() {

            StepVerifier.FirstStep<List<ServiceInfo>> servicesVerifier = StepVerifier.create(gatewayIndexService.indexGatewayServices(eurekaInstance));

            servicesVerifier
                .expectNext(asList(serviceInfoA, serviceInfoB))
                .verifyComplete();

            verify(exchangeFunction).exchange(any());

            Map<String, List<ServiceInfo>> allServices = gatewayIndexService.listRegistry(null, null, null);

            assertThat(allServices).containsOnlyKeys("testApimlIdA");
            assertThat(allServices.get("testApimlIdA")).containsExactlyInAnyOrder(serviceInfoA, serviceInfoB);
            verifyNoMoreInteractions(exchangeFunction);
        }

        @Test
        void shouldGenerateSyntheticApimlIdCacheKey() {
            when(eurekaInstance.getMetadata()).thenReturn(null);

            StepVerifier.FirstStep<List<ServiceInfo>> servicesVerifier = StepVerifier.create(gatewayIndexService.indexGatewayServices(eurekaInstance));

            servicesVerifier
                .expectNext(asList(serviceInfoA, serviceInfoB))
                .verifyComplete();

            Map<String, List<ServiceInfo>> allServices = gatewayIndexService.listRegistry(null, null, null);

            assertThat(allServices).containsOnlyKeys("SUBSTITUTE_testInstanceIdA");
        }

        @Test
        void shouldFilterCachedServicesByApiId() {

            StepVerifier.create(gatewayIndexService.indexGatewayServices(eurekaInstance))
                .expectNext(asList(serviceInfoA, serviceInfoB))
                .verifyComplete();

            Map<String, List<ServiceInfo>> allServices = gatewayIndexService.listRegistry(null, apiCatalogApiId, null);

            assertThat(allServices).containsOnly(new AbstractMap.SimpleEntry<>("testApimlIdA", Collections.singletonList(serviceInfoB)));
        }

        @Test
        void shouldFilterCachedServicesByServiceId() {

            StepVerifier.create(gatewayIndexService.indexGatewayServices(eurekaInstance))
                .expectNext(asList(serviceInfoA, serviceInfoB))
                .verifyComplete();

            Map<String, List<ServiceInfo>> allServices = gatewayIndexService.listRegistry(null, null, serviceId);

            assertThat(allServices).containsOnly(new AbstractMap.SimpleEntry<>("testApimlIdA", Collections.singletonList(serviceInfoB)));
        }

        @Test
        void shouldFilterOutEmptyApimlEntriesServiceId() {

            StepVerifier.create(gatewayIndexService.indexGatewayServices(eurekaInstance))
                .expectNext(asList(serviceInfoA, serviceInfoB))
                .verifyComplete();

            Map<String, List<ServiceInfo>> allServices = gatewayIndexService.listRegistry(null, null, "not_existing_serviceId");

            assertThat(allServices).isEmpty();
        }

        @Test
        void shouldFilterOutEmptyApimlEntriesApiId() {

            StepVerifier.create(gatewayIndexService.indexGatewayServices(eurekaInstance))
                .expectNext(asList(serviceInfoA, serviceInfoB))
                .verifyComplete();

            Map<String, List<ServiceInfo>> allServices = gatewayIndexService.listRegistry(null, "not_existing_apiId", null);

            assertThat(allServices).isEmpty();
        }

        @Test
        void shouldReturnEmptyMapForNotExistingApimlId() {
            assertThat(gatewayIndexService.listRegistry("unknownId", null, null)).isEmpty();
            assertThat(gatewayIndexService.listRegistry(null, "unknownApiId", null)).isEmpty();
            assertThat(gatewayIndexService.listRegistry("unknownId", "unknownApiId", null)).isEmpty();
        }
    }

    @Nested
    class WhenUsingCustomClientKey {

        @Test
        void shouldInitializeCustomSslContext() {

            gatewayIndexService = new GatewayIndexService(webClient, 60, KEYSTORE_PATH, PASSWORD, "PKCS12");

            SslContext customClientSslContext = (SslContext) ReflectionTestUtils.getField(gatewayIndexService, "customClientSslContext");

            assertThat(customClientSslContext).isNotNull();
        }

        @Test
        void shouldNotUseDefaultWebClientWhenCustomContextIdProvided() {
            gatewayIndexService = new GatewayIndexService(webClient, 60, KEYSTORE_PATH, PASSWORD, "PKCS12");

            StepVerifier.create(gatewayIndexService.indexGatewayServices(eurekaInstance))
                .verifyComplete();

            verifyNoInteractions(webClient);
        }

        @Test
        void shouldSkipCustomSslContextCreationIfPasswordNotDefined() {

            gatewayIndexService = new GatewayIndexService(webClient, 60, KEYSTORE_PATH, null, null);

            SslContext customClientSslContext = (SslContext) ReflectionTestUtils.getField(gatewayIndexService, "customClientSslContext");

            assertThat(customClientSslContext).isNull();
        }

        @Test
        void shouldUseDefaultWebClientWhenCustomSslContextIsNotProvided() {
            gatewayIndexService = new GatewayIndexService(webClient, 60, null, null, null);

            StepVerifier.create(gatewayIndexService.indexGatewayServices(eurekaInstance))
                .verifyComplete();

            verify(webClient).mutate();
        }

    }
}
