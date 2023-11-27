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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.discovery.ServiceRouteMapper;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.zowe.apiml.gateway.filters.pre.LocationFilter;
import org.zowe.apiml.product.routing.RoutedServicesUser;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.*;

@ExtendWith(SpringExtension.class)
class ApimlRouteLocatorTest {

    ApimlRouteLocator underTest;

    @Mock
    LocationFilter user1;
    @Mock
    LocationFilter user2;

    @Mock
    EurekaDiscoveryClient eurekaDiscoveryClient;

    @Mock
    ServiceRouteMapper serviceRouteMapper;

    private final List<RoutedServicesUser> routedServicesUserList = new ArrayList<>();

    @BeforeEach
    void setup() {
        routedServicesUserList.add(user1);
        routedServicesUserList.add(user2);

        underTest = new ApimlRouteLocator("", eurekaDiscoveryClient, new ZuulProperties(), serviceRouteMapper, routedServicesUserList);
    }

    @Nested
    class GivenServiceIsIgnored {

        @BeforeEach
        void setup() {
            ZuulProperties properties = new ZuulProperties();
            properties.setIgnoredServices(Collections.singleton("discovery"));
            underTest = new ApimlRouteLocator("", eurekaDiscoveryClient, properties, serviceRouteMapper, routedServicesUserList);
        }

        @Test
        void whenLocateRoutes_thenIgnoreService() {
            Map<String, String> metadata = new HashMap<>();
            metadata.put(ROUTES + ".ws-v1." + ROUTES_GATEWAY_URL, "ws/v1");
            metadata.put(ROUTES + ".ws-v1." + ROUTES_SERVICE_URL, "/");
            metadata.put(ROUTES + ".api-v1." + ROUTES_GATEWAY_URL, "api/v1");
            metadata.put(ROUTES + ".api-v1." + ROUTES_SERVICE_URL, "/");

            when(eurekaDiscoveryClient.getServices()).thenReturn(ImmutableList.of("discovery", "service"));
            when(eurekaDiscoveryClient.getInstances("service")).thenReturn(
                Collections.singletonList(new DefaultServiceInstance("localhost:service:80", "service", "localhost", 80, false, metadata)));
            when(eurekaDiscoveryClient.getInstances("discovery")).thenReturn(
                Collections.singletonList(new DefaultServiceInstance("localhost:discovery:80", "discovery", "localhost", 80, false, metadata)));
            when(serviceRouteMapper.apply("service")).thenReturn("service");
            when(serviceRouteMapper.apply("discovery")).thenReturn("discovery");

            Map<String, ZuulProperties.ZuulRoute> zuulRouteMap = underTest.locateRoutes();
            assertEquals(2, zuulRouteMap.size());
            assertFalse(zuulRouteMap.containsKey("/discovery/api/v1/**"));
            assertFalse(zuulRouteMap.containsKey("/discovery/ws/v1/**"));
            assertTrue(zuulRouteMap.containsKey("/service/api/v1/**"));
            assertTrue(zuulRouteMap.containsKey("/service/ws/v1/**"));
        }
    }

    @Nested
    class GivenOneServiceWithMultipleRoutes {
        @Test
        void whenLocateRoutes_thenRoutesLocate() {

            Map<String, String> metadata = new HashMap<>();
            metadata.put(ROUTES + ".ws-v1." + ROUTES_GATEWAY_URL, "ws/v1");
            metadata.put(ROUTES + ".ws-v1." + ROUTES_SERVICE_URL, "/");

            ZuulProperties.ZuulRoute expectedRoute = new ZuulProperties.ZuulRoute();
            expectedRoute.setId("service/ws/v1");
            expectedRoute.setPath("/service/ws/v1/**");
            expectedRoute.setServiceId("service");

            LinkedHashMap<String, ZuulProperties.ZuulRoute> expectedRoutesMap = new LinkedHashMap<>();

            expectedRoutesMap.put("/service/ws/v1/**", expectedRoute);

            metadata.put(ROUTES + ".api-v1." + ROUTES_GATEWAY_URL, "api/v1");
            metadata.put(ROUTES + ".api-v1." + ROUTES_SERVICE_URL, "/");
            expectedRoute = new ZuulProperties.ZuulRoute();
            expectedRoute.setId("service/api/v1");
            expectedRoute.setPath("/service/api/v1/**");
            expectedRoute.setServiceId("service");

            expectedRoutesMap.put("/service/api/v1/**", expectedRoute);

            when(eurekaDiscoveryClient.getServices()).thenReturn(Collections.singletonList("service"));
            when(eurekaDiscoveryClient.getInstances("service")).thenReturn(
                Collections.singletonList(new DefaultServiceInstance("localhost:service:80", "service", "localhost", 80, false, metadata)));
            when(serviceRouteMapper.apply("service")).thenReturn("service");

            Map<String, ZuulProperties.ZuulRoute> zuulRouteMap = underTest.locateRoutes();
            assertEquals(expectedRoutesMap, zuulRouteMap);
        }

        @Test
        void whenInstanceIsNull_thenContinue() {
            ZuulProperties.ZuulRoute expectedRoute;

            LinkedHashMap<String, ZuulProperties.ZuulRoute> expectedRoutesMap = new LinkedHashMap<>();

            expectedRoute = new ZuulProperties.ZuulRoute();
            expectedRoute.setId("service");
            expectedRoute.setPath("/service/**");
            expectedRoute.setServiceId("service");

            expectedRoutesMap.put("/service/**", expectedRoute);

            when(eurekaDiscoveryClient.getServices()).thenReturn(Collections.singletonList("service"));
            when(eurekaDiscoveryClient.getInstances("service")).thenReturn(null);
            when(serviceRouteMapper.apply("service")).thenReturn("service");

            Map<String, ZuulProperties.ZuulRoute> zuulRouteMap = underTest.locateRoutes();
            assertEquals(expectedRoutesMap, zuulRouteMap);
        }

        @Test
        void whenInstanceIsEmpty_thenContinue() {
            ZuulProperties.ZuulRoute expectedRoute;

            LinkedHashMap<String, ZuulProperties.ZuulRoute> expectedRoutesMap = new LinkedHashMap<>();

            expectedRoute = new ZuulProperties.ZuulRoute();
            expectedRoute.setId("service");
            expectedRoute.setPath("/service/**");
            expectedRoute.setServiceId("service");

            expectedRoutesMap.put("/service/**", expectedRoute);

            when(eurekaDiscoveryClient.getServices()).thenReturn(Collections.singletonList("service"));
            when(eurekaDiscoveryClient.getInstances("service")).thenReturn(Collections.emptyList());
            when(serviceRouteMapper.apply("service")).thenReturn("service");

            Map<String, ZuulProperties.ZuulRoute> zuulRouteMap = underTest.locateRoutes();
            assertEquals(expectedRoutesMap, zuulRouteMap);
        }

        @Test
        void whenNoSlashInPath_thenAppendIt() {
            ZuulProperties zuulProperties = new ZuulProperties();
            ZuulProperties.ZuulRoute zuulRoute = new ZuulProperties.ZuulRoute();

            zuulRoute.setId("/service/**");
            zuulRoute.setPath("service/**");
            zuulRoute.setServiceId("service");
            Map<String, ZuulProperties.ZuulRoute> expectedMap = new HashMap<>();
            expectedMap.put("/service/**", zuulRoute);
            zuulProperties.setRoutes(expectedMap);
            underTest = new ApimlRouteLocator("", eurekaDiscoveryClient, zuulProperties, serviceRouteMapper, routedServicesUserList);
            Map<String, String> metadata = new HashMap<>();
            metadata.put(ROUTES + ".ws-v1." + ROUTES_GATEWAY_URL, "ws/v1");
            metadata.put(ROUTES + ".ws-v1." + ROUTES_SERVICE_URL, "/");

            ZuulProperties.ZuulRoute expectedRoute = new ZuulProperties.ZuulRoute();
            expectedRoute.setId("service/ws/v1");
            expectedRoute.setPath("/service/ws/v1/**");
            expectedRoute.setServiceId("service");

            LinkedHashMap<String, ZuulProperties.ZuulRoute> expectedRoutesMap = new LinkedHashMap<>();

            expectedRoutesMap.put("/service/ws/v1/**", expectedRoute);
            when(eurekaDiscoveryClient.getServices()).thenReturn(Collections.singletonList("service"));
            when(eurekaDiscoveryClient.getInstances("service")).thenReturn(
                Collections.singletonList(new DefaultServiceInstance("localhost:service:80", "service", "localhost", 80, false, metadata)));
            when(serviceRouteMapper.apply("service")).thenReturn("service");

            Map<String, ZuulProperties.ZuulRoute> zuulRouteMap = underTest.locateRoutes();
            assertEquals(expectedRoutesMap, zuulRouteMap);
        }

        @Test
        void whenPrefix_thenConstructPath() {
            ZuulProperties zuulProperties = new ZuulProperties();

            zuulProperties.setPrefix("/prefix");
            underTest = new ApimlRouteLocator("", eurekaDiscoveryClient, zuulProperties, serviceRouteMapper, routedServicesUserList);
            Map<String, String> metadata = new HashMap<>();
            metadata.put(ROUTES + ".ws-v1." + ROUTES_GATEWAY_URL, "ws/v1");

            ZuulProperties.ZuulRoute expectedRoute = new ZuulProperties.ZuulRoute();
            expectedRoute.setId("service");
            expectedRoute.setPath("/service/**");
            expectedRoute.setServiceId("service");

            LinkedHashMap<String, ZuulProperties.ZuulRoute> expectedRoutesMap = new LinkedHashMap<>();

            expectedRoutesMap.put("/prefix/prefix/service/**", expectedRoute);
            when(eurekaDiscoveryClient.getServices()).thenReturn(Collections.singletonList("service"));
            when(eurekaDiscoveryClient.getInstances("service")).thenReturn(
                Collections.singletonList(new DefaultServiceInstance("localhost:service:80", "service", "localhost", 80, false, metadata)));
            when(serviceRouteMapper.apply("service")).thenReturn("service");

            Map<String, ZuulProperties.ZuulRoute> zuulRouteMap = underTest.locateRoutes();
            assertEquals(expectedRoutesMap, zuulRouteMap);
        }

        @Test
        void whenServiceIdIsNull_thenUseIdAndLocate() {
            ZuulProperties zuulProperties = new ZuulProperties();
            ZuulProperties.ZuulRoute zuulRoute = new ZuulProperties.ZuulRoute();

            zuulRoute.setPath("service/ws/v1/**");
            zuulRoute.setId("service/ws/v1");
            zuulRoute.setServiceId(null);
            Map<String, ZuulProperties.ZuulRoute> expectedMap = new HashMap<>();
            expectedMap.put("service", zuulRoute);
            zuulProperties.setRoutes(expectedMap);
            underTest = new ApimlRouteLocator("", eurekaDiscoveryClient, zuulProperties, serviceRouteMapper, routedServicesUserList);
            Map<String, String> metadata = new HashMap<>();
            metadata.put(ROUTES + ".ws-v1." + ROUTES_GATEWAY_URL, "ws/v1");
            metadata.put(ROUTES + ".ws-v1." + ROUTES_SERVICE_URL, "/");

            ZuulProperties.ZuulRoute expectedRoute = new ZuulProperties.ZuulRoute();
            expectedRoute.setId("service/ws/v1");
            expectedRoute.setPath("service/ws/v1/**");
            expectedRoute.setServiceId(null);

            LinkedHashMap<String, ZuulProperties.ZuulRoute> expectedRoutesMap = new LinkedHashMap<>();

            expectedRoutesMap.put("/service/ws/v1/**", expectedRoute);
            when(eurekaDiscoveryClient.getServices()).thenReturn(Collections.singletonList("service"));
            when(eurekaDiscoveryClient.getInstances("service")).thenReturn(
                Collections.singletonList(new DefaultServiceInstance("localhost:service:80", "service", "localhost", 80, false, metadata)));
            when(serviceRouteMapper.apply("service")).thenReturn("service");

            Map<String, ZuulProperties.ZuulRoute> zuulRouteMap = underTest.locateRoutes();
            assertEquals(expectedRoutesMap, zuulRouteMap);
        }
    }
}
