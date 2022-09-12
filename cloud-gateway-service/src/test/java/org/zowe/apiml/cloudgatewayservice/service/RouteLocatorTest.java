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

import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.gateway.route.RouteDefinition;
import reactor.core.publisher.Flux;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.*;

class RouteLocatorTest {

    @Test
    void givenServiceWithDefinedMetadata_thenLocateRoutes() {
        ReactiveDiscoveryClient dc = mock(ReactiveDiscoveryClient.class);
        Flux<String> services = Flux.fromIterable(Collections.singleton("gateway"));
        Map<String, String> metadata = new HashMap<>();
        metadata.put(ROUTES + ".api-v1." + ROUTES_GATEWAY_URL, "api/v1");
        metadata.put(ROUTES + ".api-v1." + ROUTES_SERVICE_URL, "/");
        ServiceInstance instance = new DefaultServiceInstance("gateway-10012", "gateway", "localhost", 10012, true, metadata);
        Flux<ServiceInstance> serviceInstances = Flux.fromIterable(Collections.singleton(instance));
        when(dc.getServices()).thenReturn(services);
        when(dc.getInstances("gateway")).thenReturn(serviceInstances);
        DiscoveryLocatorProperties properties = new DiscoveryLocatorProperties();
        RouteLocator locator = new RouteLocator(dc, properties);
        Flux<RouteDefinition> definitionFlux = locator.getRouteDefinitions();
        List<RouteDefinition> definitions = definitionFlux.collectList().block();
        assertNotNull(definitions);
        assertEquals(1, definitions.size());
    }


}
