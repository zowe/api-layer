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

import org.apache.logging.log4j.util.TriConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.cloudgatewayservice.service.routing.RouteDefinitionProducer;
import org.zowe.apiml.cloudgatewayservice.service.scheme.SchemeHandler;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.util.CorsUtils;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.APIML_ID;

class RouteLocatorTest {

    private final ServiceInstance MOCK_SERVICE = createServiceInstance("mockService");

    private static final FilterDefinition[] COMMON_FILTERS = {
        new FilterDefinition(), new FilterDefinition()
    };
    private static final SchemeHandler[] SCHEME_HANDLER_FILTERS = {
        createSchemeHandler(AuthenticationScheme.BYPASS), createSchemeHandler(AuthenticationScheme.ZOWE_JWT)
    };
    private static final RouteDefinitionProducer[] PRODUCERS = {
        createRouteDefinitionProducer(5, "id5"),
        createRouteDefinitionProducer(0, "id0"),
        createRouteDefinitionProducer(10, "id10")
    };

    private UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = mock(UrlBasedCorsConfigurationSource.class);
    private CorsUtils corsUtils = mock(CorsUtils.class);
    private ReactiveDiscoveryClient discoveryClient = mock(ReactiveDiscoveryClient.class);

    private RouteLocator routeLocator;

    @BeforeEach
    void init() {
        ApplicationContext context = mock(ApplicationContext.class);
        doReturn(urlBasedCorsConfigurationSource).when(context).getBean(UrlBasedCorsConfigurationSource.class);

        routeLocator = spy(new RouteLocator(
            context,
            corsUtils,
            discoveryClient,
            Arrays.asList(COMMON_FILTERS),
            Arrays.asList(SCHEME_HANDLER_FILTERS),
            Arrays.asList(PRODUCERS)
        ));
    }

    private ServiceInstance createServiceInstance(String serviceId, String...routes) {
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        doReturn(serviceId).when(serviceInstance).getServiceId();

        Map<String, String> metadata = new HashMap<>();
        int i = 1;
        for (String route : routes) {
            metadata.put("apiml.routes.api-v" + i + ".gatewayUrl", route);
            metadata.put("apiml.routes.api-v" + i + ".serviceUrl", route);
            i++;
        }
        doReturn(metadata).when(serviceInstance).getMetadata();

        return serviceInstance;
    }

    private static SchemeHandler createSchemeHandler(AuthenticationScheme type) {
        SchemeHandler out = mock(SchemeHandler.class);
        doReturn(type).when(out).getAuthenticationScheme();
        return out;
    }

    private static RouteDefinitionProducer createRouteDefinitionProducer(int order, String id) {
        EurekaMetadataParser metadataParser = new EurekaMetadataParser();
        RouteDefinitionProducer rdp = mock(RouteDefinitionProducer.class);
        doReturn(order).when(rdp).getOrder();
        doAnswer(answer -> {
            ServiceInstance serviceInstance = answer.getArgument(0);
            RoutedService routedService = answer.getArgument(1);

            RouteDefinition routeDefinition = new RouteDefinition();
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("serviceId", serviceInstance.getServiceId());
            metadata.put("gatewayUrl", routedService.getGatewayUrl());
            metadata.put("producerId", id);
            routeDefinition.setMetadata(metadata);

            return routeDefinition;
        }).when(rdp).get(any(), any());
        return rdp;
    }

    @Nested
    class CommonMethods {

        @Test
        void givenDiscoveryClient_whenGetServiceInstances_thenReturnAllServiceInstances() {
            when(discoveryClient.getServices()).thenReturn(Flux.fromArray(new String[] {"service1", "service2"}));
            ServiceInstance serviceInstance1 = mock(ServiceInstance.class);
            ServiceInstance serviceInstance2 = mock(ServiceInstance.class);
            doReturn(Flux.just(serviceInstance1)).when(discoveryClient).getInstances("service1");
            doReturn(Flux.just(serviceInstance2)).when(discoveryClient).getInstances("service2");

            doCallRealMethod().when(routeLocator).getServiceInstances();
            assertArrayEquals(
                new Object[] {Collections.singletonList(serviceInstance1), Collections.singletonList(serviceInstance2)},
                routeLocator.getServiceInstances().toStream().toArray()
            );
        }

        @Test
        void givenNoAuthentication_whenSetAuth_thenDoNothing() {
            assertDoesNotThrow(() -> routeLocator.setAuth(MOCK_SERVICE,null, null));
        }

        @Test
        void givenNoAuthenticationScheme_whenSetAuth_thenDoNothing() {
            assertDoesNotThrow(() -> routeLocator.setAuth(MOCK_SERVICE, null, new Authentication()));
        }

        @Test
        void givenAuthenticationSchemeWithoutFilter_whenSetAuth_thenDoNothing() {
            assertDoesNotThrow(() -> routeLocator.setAuth(MOCK_SERVICE, null, new Authentication(AuthenticationScheme.X509, null)));
        }

        @Test
        void givenExistingAuthenticationScheme_whenSetAuth_thenCallApply() {
            RouteDefinition routeDefinition = mock(RouteDefinition.class);
            Authentication authentication = new Authentication(AuthenticationScheme.BYPASS, null);

            routeLocator.setAuth(MOCK_SERVICE, routeDefinition, authentication);

            verify(SCHEME_HANDLER_FILTERS[0]).apply(MOCK_SERVICE, routeDefinition, authentication);
        }

        private TriConsumer<String, String, CorsConfiguration> getCorsLambda(Consumer<Map<String, String>> metadataProcessor) {
            ServiceInstance serviceInstance = createServiceInstance("myservice", "api/v1");
            metadataProcessor.accept(serviceInstance.getMetadata());

            routeLocator.setCors(serviceInstance);
            ArgumentCaptor<TriConsumer<String, String, CorsConfiguration>> lambdaCaptor = ArgumentCaptor.forClass(TriConsumer.class);
            verify(corsUtils).setCorsConfiguration(anyString(), any(), lambdaCaptor.capture());

            return lambdaCaptor.getValue();
        }

        @Test
        void givenApimlId_whenSetCors_thenServiceIdIsReplacedWithApimlId() {
            TriConsumer<String, String, CorsConfiguration> corsLambda = getCorsLambda(md -> md.put(APIML_ID, "apimlid"));

            corsLambda.accept(null, "myservice", null);

            verify(urlBasedCorsConfigurationSource).registerCorsConfiguration("/apimlid/**", null);
        }

        @Test
        void givenNoApimlId_whenSetCors_thenServiceIdIsUsed() {
            TriConsumer<String, String, CorsConfiguration> corsLambda = getCorsLambda(md -> {});

            corsLambda.accept(null, "myservice", null);

            verify(urlBasedCorsConfigurationSource).registerCorsConfiguration("/myservice/**", null);
        }

        @Test
        void givenGateway_whenGetRoutedService_thenReturnDefaultRouting() {
            ServiceInstance gw = createServiceInstance("gateway", "api/v1");
            List<RoutedService> rs = routeLocator.getRoutedService(gw).collect(Collectors.toList());
            assertEquals(1, rs.size());
            assertEquals("", rs.get(0).getGatewayUrl());
            assertEquals("/", rs.get(0).getServiceUrl());
        }

        @Test
        void givenNonGatewayService_whenGetRoutedService_thenReturnRoutingFromMetadata() {
            ServiceInstance s = createServiceInstance("myservice", "api/v1", "ui/v1");
            List<RoutedService> rs = routeLocator.getRoutedService(s).collect(Collectors.toList());
            assertEquals(2, rs.size());
        }

    }

    @Nested
    class Generating {

        @Test
        void givenRouteLocator_whenGetRouteDefinitions_thenGenerateAll() {
            doReturn(Flux.fromIterable(Arrays.asList(
                Arrays.asList(
                    createServiceInstance("service1", "/", "/a/b"),
                    createServiceInstance("service2", "/a/b", "/")
                )
            ))).when(routeLocator).getServiceInstances();

            RouteDefinition[] rds = routeLocator.getRouteDefinitions().toStream().toArray(RouteDefinition[]::new);

            int index = 0;
            for (String serviceId : new String[] {"service1", "service2"}) {
                verify(corsUtils).setCorsConfiguration(eq(serviceId), any(), any());

                for (String gatewayUrl : new String[] {"a/b", ""}) {
                    for (String producerId : new String[] {"id0", "id5", "id10"}) {
                        assertEquals(index, rds[index].getOrder());
                        assertEquals(serviceId, rds[index].getMetadata().get("serviceId"));
                        assertEquals(gatewayUrl, rds[index].getMetadata().get("gatewayUrl"));
                        assertEquals(producerId, rds[index].getMetadata().get("producerId"));
                        index++;
                    }
                }
            }
        }

    }

}
