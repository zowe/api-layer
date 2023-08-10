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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.zowe.apiml.util.CorsUtils;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.*;

class RouteLocatorTest {

    static Map<String, String> metadata = new HashMap<>();

    static {
        metadata.put(ROUTES + ".api-v1." + ROUTES_GATEWAY_URL, "api/v1");
        metadata.put(ROUTES + ".api-v1." + ROUTES_SERVICE_URL, "/");
    }

    ServiceInstance instance = new DefaultServiceInstance("gateway-10012", "gateway", "gatewayhost", 10012, true, metadata);
    ServiceInstance instance2 = new DefaultServiceInstance("gateway-2-10012", "gateway", "gatewayhost-2", 10012, true, metadata);
    ReactiveDiscoveryClient dc = mock(ReactiveDiscoveryClient.class);
    DiscoveryLocatorProperties properties = new DiscoveryLocatorProperties();

//    @Nested
//    class GivenRouteLocator {
//        @Test
//        void givenServiceWithDefinedMetadata_thenLocateRoutes() {
//            Flux<String> services = Flux.fromIterable(Collections.singleton("gateway"));
//            Flux<ServiceInstance> serviceInstances = Flux.fromIterable(Collections.singleton(instance));
//            when(dc.getServices()).thenReturn(services);
//            when(dc.getInstances("gateway")).thenReturn(serviceInstances);
//            CorsUtils corsUtils = new CorsUtils(false);
//            RouteLocator locator = new RouteLocator(dc, properties, Collections.singletonList(new FilterDefinition("name=value")), null, corsUtils);
//            Flux<RouteDefinition> definitionFlux = locator.getRouteDefinitions();
//            List<RouteDefinition> definitions = definitionFlux.collectList().block();
//            assertNotNull(definitions);
//            assertEquals(1, definitions.size());
//        }
//    }
//
//    @Nested
//    class GivenProxyRouteLocator {
//        @Test
//        void whenServiceIsMatched_thenCreateRouteWithCorrectPredicate() {
//            Flux<String> services = Flux.fromIterable(Collections.singleton("gateway"));
//            List<ServiceInstance> instances = Arrays.asList(instance, instance2);
//            Flux<ServiceInstance> serviceInstances = Flux.fromIterable(instances);
//            when(dc.getServices()).thenReturn(services);
//            when(dc.getInstances("gateway")).thenReturn(serviceInstances);
//            CorsUtils corsUtils = new CorsUtils(false);
//            ProxyRouteLocator locator = new ProxyRouteLocator(dc, properties, Collections.emptyList(), null, corsUtils);
//            Flux<RouteDefinition> definitionFlux = locator.getRouteDefinitions();
//            List<RouteDefinition> definitions = definitionFlux.collectList().block();
//            assertNotNull(definitions);
//            assertEquals(2, definitions.size());
//            for (int i = 0; i < definitions.size(); i++) {
//                RouteDefinition def = definitions.get(i);
//                String expression = def.getPredicates().get(0).getArgs().get("regexp");
//                assertTrue(Pattern.matches(expression, instances.get(i).getServiceId() + instances.get(i).getHost()));
//            }
//        }
//    }


}
