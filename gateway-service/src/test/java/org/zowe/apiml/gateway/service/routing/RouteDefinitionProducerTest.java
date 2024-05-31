/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.service.routing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.zowe.apiml.product.routing.RoutedService;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.APIML_ID;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.SERVICE_EXTERNAL_URL;

class RouteDefinitionProducerTest {

    private static final String EXTERNAL_URL = "https://external.url";

    private DiscoveryLocatorProperties properties = new DiscoveryLocatorProperties();
    private final RouteDefinitionProducer routeDefinitionProducer = mock(
        RouteDefinitionProducer.class, withSettings().useConstructor(properties).defaultAnswer(CALLS_REAL_METHODS)
    );

    ServiceInstance serviceInstance;

    @BeforeEach
    void initServiceInstance() {
        serviceInstance = mock(ServiceInstance.class);
        doReturn("myservice").when(serviceInstance).getServiceId();
    }

    @Nested
    class HostnameEvaluation {


        @Test
        void givenServiceInstance_whenEvalHostname_thenConstructUrl() {
            assertEquals("lb://myservice", routeDefinitionProducer.evalHostname(serviceInstance));
        }

        @Test
        void givenExternalUrl_whenGetHostname_thenReturnExternalUrl() {
            doReturn(Collections.singletonMap(SERVICE_EXTERNAL_URL, EXTERNAL_URL)).when(serviceInstance).getMetadata();
            assertEquals(EXTERNAL_URL, routeDefinitionProducer.getHostname(serviceInstance));
        }

        @Test
        void givenNoExternalUrl_whenGetHostname_thenReturnDefaultUrl() {
            assertEquals("lb://myservice", routeDefinitionProducer.getHostname(serviceInstance));
        }

        @Test
        void givenApimlId_whenGetEvalServiceInstance_thenMockGetServiceId() {
            doReturn(Collections.singletonMap(APIML_ID, "myinstance")).when(serviceInstance).getMetadata();
            ServiceInstance modified = routeDefinitionProducer.getEvalServiceInstance(serviceInstance);
            assertNotSame(modified, serviceInstance);
            assertEquals("myinstance", modified.getServiceId());
        }

        @Test
        void givenNoApimlId_whenGetEvalServiceInstance_thenReturnLowerCaseCopy() {
            doReturn("myService").when(serviceInstance).getServiceId();
            ServiceInstance modified = routeDefinitionProducer.getEvalServiceInstance(serviceInstance);
            assertNotSame(modified, serviceInstance);
            assertEquals("myservice", modified.getServiceId());
        }

    }

    @Nested
    class ConstructRule {

        @Test
        void givenInstanceData_whenGet_thenConstructDraftOfRule() {
            RoutedService routedService = mock(RoutedService.class);
            doReturn("routeId").when(routedService).getSubServiceId();
            doReturn(Collections.singletonMap(SERVICE_EXTERNAL_URL, EXTERNAL_URL)).when(serviceInstance).getMetadata();
            doReturn(123).when(routeDefinitionProducer).getOrder();
            doReturn("original:instance:id").when(serviceInstance).getInstanceId();
            ServiceInstance serviceInstanceModified = new RouteDefinitionProducer.ServiceInstanceEval(serviceInstance, "anotherId");
            doReturn(serviceInstanceModified).when(routeDefinitionProducer).getEvalServiceInstance(serviceInstance);

            RouteDefinition routeDefinition = routeDefinitionProducer.get(serviceInstance, routedService);

            assertEquals(123, routeDefinition.getOrder());
            assertEquals("anotherId", serviceInstanceModified.getServiceId());
            assertEquals("original:instance:id:routeId", routeDefinition.getId());
            assertEquals(EXTERNAL_URL, routeDefinition.getUri().toString());
            assertArrayEquals(routeDefinition.getMetadata().entrySet().toArray(), serviceInstance.getMetadata().entrySet().toArray());
            verify(routeDefinitionProducer).setCondition(routeDefinition, serviceInstanceModified, routedService);
            verify(routeDefinitionProducer).setFilters(routeDefinition, serviceInstanceModified, routedService);
        }

    }

}
