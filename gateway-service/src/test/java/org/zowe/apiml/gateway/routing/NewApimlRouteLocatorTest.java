/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.routing;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.*;

@ExtendWith(SpringExtension.class)
class NewApimlRouteLocatorTest {

    NewApimlRouteLocator underTest;

    @Mock
    EurekaDiscoveryClient eurekaDiscoveryClient;

    @Mock
    RoutedServicesNotifier routedServicesNotifier;

    @BeforeEach
    void setup() {
        underTest = new NewApimlRouteLocator("", new ZuulProperties(), eurekaDiscoveryClient, routedServicesNotifier);
    }

    @Test
    void givenOneServiceWithMultipleRoutes_whenLocateRoutes_thenRoutesLocatedWithOneFormat() {
        //given
        Map<String, String> metadata = new HashMap<>();
        metadata.put(ROUTES + ".api-v1." + ROUTES_GATEWAY_URL, "api/v1");
        metadata.put(ROUTES + ".api-v1." + ROUTES_SERVICE_URL, "/");

        ZuulProperties.ZuulRoute expectedRoute = new ZuulProperties.ZuulRoute();
        expectedRoute.setId("service/api/v1");
        expectedRoute.setPath("/service/api/v1/**");
        expectedRoute.setServiceId("service");

        LinkedHashMap<String, ZuulProperties.ZuulRoute> expectedRoutesMap = new LinkedHashMap<>();
        expectedRoutesMap.put("/service/api/v1/**", expectedRoute);

        metadata.put(ROUTES + ".ws-v1." + ROUTES_GATEWAY_URL, "ws/v1");
        metadata.put(ROUTES + ".ws-v1." + ROUTES_SERVICE_URL, "/");

        expectedRoute = new ZuulProperties.ZuulRoute();
        expectedRoute.setId("service/ws/v1");
        expectedRoute.setPath("/service/ws/v1/**");
        expectedRoute.setServiceId("service");

        expectedRoutesMap.put("/service/ws/v1/**", expectedRoute);

        when(eurekaDiscoveryClient.getServices()).thenReturn(Collections.singletonList("service"));
        when(eurekaDiscoveryClient.getInstances("service")).thenReturn(
            Collections.singletonList(new DefaultServiceInstance("localhost:service:80", "service", "localhost", 80, false, metadata)));
        //when
        Map<String, ZuulProperties.ZuulRoute> zuulRouteMap = underTest.locateRoutes();
        //then
        assertEquals(expectedRoutesMap, zuulRouteMap);
    }

    @Test
    void whenRouteLocated_thenNotifierInvoked() {
        //when
        underTest.locateRoutes();
        //then
        verify(routedServicesNotifier, times(1)).notifyAndFlush();
    }

    @Test
    void givenZuulIgnoredService_whenLocateRoutes_thenNotInRoutes() {
        ReflectionTestUtils.setField(underTest, "zuulIgnored", new String[]{"discovery"});
        Map<String, String> metadata = new HashMap<>();
        metadata.put(ROUTES + ".api-v1." + ROUTES_GATEWAY_URL, "api/v1");
        metadata.put(ROUTES + ".api-v1." + ROUTES_SERVICE_URL, "/");
        metadata.put(ROUTES + ".ws-v1." + ROUTES_GATEWAY_URL, "ws/v1");
        metadata.put(ROUTES + ".ws-v1." + ROUTES_SERVICE_URL, "/");

        when(eurekaDiscoveryClient.getServices()).thenReturn(ImmutableList.of("discovery", "service"));
        when(eurekaDiscoveryClient.getInstances("service")).thenReturn(
            Collections.singletonList(new DefaultServiceInstance("localhost:service:80", "service", "localhost", 80, false, metadata)));
        when(eurekaDiscoveryClient.getInstances("discovery")).thenReturn(
            Collections.singletonList(new DefaultServiceInstance("localhost:discovery:80", "discovery", "localhost", 80, false, metadata)));

        Map<String, ZuulProperties.ZuulRoute> zuulRouteMap = underTest.locateRoutes();
        assertEquals(2, zuulRouteMap.size());
        assertFalse(zuulRouteMap.containsKey("/discovery/api/v1/**"));
        assertFalse(zuulRouteMap.containsKey("/discovery/ws/v1/**"));
        assertTrue(zuulRouteMap.containsKey("/service/api/v1/**"));
        assertTrue(zuulRouteMap.containsKey("/service/ws/v1/**"));
    }

}
