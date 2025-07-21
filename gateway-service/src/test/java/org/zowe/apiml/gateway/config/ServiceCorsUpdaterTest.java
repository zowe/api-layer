/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import org.apache.logging.log4j.util.TriConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.handler.RoutePredicateHandlerMapping;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.zowe.apiml.constants.EurekaMetadataDefinition;
import org.zowe.apiml.util.CorsUtils;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ServiceCorsUpdaterTest {

    private static final String SERVICE_ID = "myserviceid";
    private static final String APIML_ID = "apimlid";

    private CorsUtils corsUtils = spy(new CorsUtils(true, Collections.emptyList()));

    @Mock private ReactiveDiscoveryClient discoveryClient;

    private ServiceCorsUpdater serviceCorsUpdater;

    private UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource;

    @BeforeEach
    void setUp() {
        serviceCorsUpdater = new ServiceCorsUpdater(corsUtils, discoveryClient, mock(RoutePredicateHandlerMapping.class), mock(GlobalCorsProperties.class));
        serviceCorsUpdater.initCorsConfigurationSource();
        urlBasedCorsConfigurationSource = spy((UrlBasedCorsConfigurationSource) ReflectionTestUtils.getField(serviceCorsUpdater, "urlBasedCorsConfigurationSource"));
        ReflectionTestUtils.setField(serviceCorsUpdater, "urlBasedCorsConfigurationSource", urlBasedCorsConfigurationSource);
    }

    private ServiceInstance createServiceInstance(String serviceId) {
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        doReturn(serviceId).when(serviceInstance).getServiceId();
        doReturn(Flux.just(serviceInstance)).when(discoveryClient).getInstances(serviceId);

        Map<String, String> metadata = new HashMap<>();

        metadata.put("apiml.routes.api-v1.gatewayUrl", "api/v1");
        metadata.put("apiml.routes.api-v1.serviceUrl", "api/v1");

        doReturn(metadata).when(serviceInstance).getMetadata();

        return serviceInstance;
    }

    @SuppressWarnings("unchecked")
    private TriConsumer<String, String, CorsConfiguration> getCorsLambda(Consumer<Map<String, String>> metadataProcessor) {
        var serviceInstance = createServiceInstance(SERVICE_ID);
        metadataProcessor.accept(serviceInstance.getMetadata());

        doReturn(Flux.just(SERVICE_ID)).when(discoveryClient).getServices();

        StepVerifier.create(serviceCorsUpdater.onRefreshRoutesEvent(new RefreshRoutesEvent(this)))
            .verifyComplete();
        ArgumentCaptor<TriConsumer<String, String, CorsConfiguration>> lambdaCaptor = ArgumentCaptor.forClass(TriConsumer.class);
        verify(corsUtils).setCorsConfiguration(anyString(), any(), lambdaCaptor.capture());

        return lambdaCaptor.getValue();
    }

    @Test
    void givenApimlId_whenSetCors_thenServiceIdIsReplacedWithApimlId() {
        TriConsumer<String, String, CorsConfiguration> corsLambda = getCorsLambda(md -> md.put(EurekaMetadataDefinition.APIML_ID, APIML_ID));

        corsLambda.accept(null, SERVICE_ID, null);

        verify(serviceCorsUpdater.getUrlBasedCorsConfigurationSource()).registerCorsConfiguration("/" + APIML_ID + "/**", null);
    }

    @Test
    void givenNoApimlId_whenSetCors_thenServiceIdIsUsed() {
        TriConsumer<String, String, CorsConfiguration> corsLambda = getCorsLambda(md -> {});

        corsLambda.accept(null, SERVICE_ID, null);

        verify(serviceCorsUpdater.getUrlBasedCorsConfigurationSource()).registerCorsConfiguration("/" + SERVICE_ID + "/**", null);
    }

}
