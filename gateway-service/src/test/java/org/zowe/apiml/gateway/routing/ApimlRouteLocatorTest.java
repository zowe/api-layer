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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.discovery.ServiceRouteMapper;
import org.zowe.apiml.gateway.filters.pre.LocationFilter;
import org.zowe.apiml.product.routing.RoutedServicesUser;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.*;

class ApimlRouteLocatorTest {

    private ApimlRouteLocator apimlRouteLocator;
    private DiscoveryClient discovery;
    private ZuulProperties properties;
    private List<RoutedServicesUser> routedServicesUsers;
    private ServiceRouteMapper serviceRouteMapper;
    private LocationFilter locationFilter;

    @BeforeEach
    public void setup() {
        discovery = mock(DiscoveryClient.class);
        properties = mock(ZuulProperties.class);
        serviceRouteMapper = mock(ServiceRouteMapper.class);
        locationFilter = mock(LocationFilter.class);

        routedServicesUsers = new ArrayList<>();
    }

    @Test
    public void shouldLocateRoutes() {
        apimlRouteLocator = new ApimlRouteLocator("service", discovery, properties, serviceRouteMapper, routedServicesUsers);

        Map<String, String> metadata = new HashMap<>();
        metadata.put(ROUTES + ".api-v1." + ROUTES_GATEWAY_URL, "api/v1");
        metadata.put(ROUTES + ".api-v1." + ROUTES_SERVICE_URL, "/");

        when(discovery.getServices()).thenReturn(Collections.singletonList("service"));
        when(discovery.getInstances("service")).thenReturn(
            Collections.singletonList(new DefaultServiceInstance("service", "localhost", 80, false, metadata)));
        when(serviceRouteMapper.apply("service")).thenReturn("service");

        LinkedHashMap<String, ZuulProperties.ZuulRoute> routes = apimlRouteLocator.locateRoutes();

        ZuulProperties.ZuulRoute expectZuulRoute = new ZuulProperties.ZuulRoute();
        expectZuulRoute.setId("api/v1/service");
        expectZuulRoute.setPath("/api/v1/service/**");
        expectZuulRoute.setServiceId("service");
        LinkedHashMap<String, ZuulProperties.ZuulRoute> expectedRoutes = new LinkedHashMap();
        expectedRoutes.put("/api/v1/service/**", expectZuulRoute);

        assertEquals(expectedRoutes, routes);
    }

    @Test
    public void shouldReturnNull_WhenLocateRoutes_IfServiceInstanceNull() {
        apimlRouteLocator = new ApimlRouteLocator("service", discovery, properties, serviceRouteMapper, routedServicesUsers);

        Map<String, String> metadata = new HashMap<>();
        metadata.put(ROUTES + ".api-v1." + ROUTES_GATEWAY_URL, "api/v1");
        metadata.put(ROUTES + ".api-v1." + ROUTES_SERVICE_URL, "/");

        when(discovery.getServices()).thenReturn(Collections.singletonList("service"));
        when(discovery.getInstances("service")).thenReturn(null);

        LinkedHashMap<String, ZuulProperties.ZuulRoute> routes = apimlRouteLocator.locateRoutes();


        assertEquals(null, routes);
    }

    @Test
    public void shouldPopulateRoutesMap_WhenLocateRoutes_IfServiceIdDifferentFromServletPath() {
        apimlRouteLocator = new ApimlRouteLocator("service", discovery, properties, serviceRouteMapper, routedServicesUsers);

        Map<String, String> metadata = new HashMap<>();
        metadata.put(ROUTES + ".api-v1." + ROUTES_GATEWAY_URL, "api/v1");
        metadata.put(ROUTES + ".api-v1." + ROUTES_SERVICE_URL, "/");

        when(discovery.getServices()).thenReturn(Collections.singletonList("differentId"));
        when(discovery.getInstances("differentId")).thenReturn(
            Collections.singletonList(new DefaultServiceInstance("differentId", "localhost", 80, false, metadata)));
        when(serviceRouteMapper.apply("differentId")).thenReturn("differentId");

        LinkedHashMap<String, ZuulProperties.ZuulRoute> routes = apimlRouteLocator.locateRoutes();

        ZuulProperties.ZuulRoute expectZuulRoute = new ZuulProperties.ZuulRoute();
        expectZuulRoute.setId("api/v1/differentId");
        expectZuulRoute.setPath("/api/v1/differentId/**");
        expectZuulRoute.setServiceId("differentId");
        LinkedHashMap<String, ZuulProperties.ZuulRoute> expectedRoutes = new LinkedHashMap();
        expectedRoutes.put("/api/v1/differentId/**", expectZuulRoute);

        assertEquals(expectedRoutes, routes);
    }

    @Test
    public void shouldConcatenatePrefix_WhenLocateRoutes_IfPrefixIsPresent() {
        apimlRouteLocator = new ApimlRouteLocator("service", discovery, properties, serviceRouteMapper, routedServicesUsers);

        Map<String, String> metadata = new HashMap<>();
        metadata.put(ROUTES + ".api-v1." + ROUTES_GATEWAY_URL, "api/v1");
        metadata.put(ROUTES + ".api-v1." + ROUTES_SERVICE_URL, "api");

        when(discovery.getServices()).thenReturn(Collections.singletonList("service"));
        when(discovery.getInstances("service")).thenReturn(
            Collections.singletonList(new DefaultServiceInstance("service", "localhost", 80, false, metadata)));
        when(serviceRouteMapper.apply("service")).thenReturn("service");
        when(properties.getPrefix()).thenReturn("prefix");

        LinkedHashMap<String, ZuulProperties.ZuulRoute> routes = apimlRouteLocator.locateRoutes();

        ZuulProperties.ZuulRoute expectZuulRoute = new ZuulProperties.ZuulRoute();
        expectZuulRoute.setId("service");
        expectZuulRoute.setPath("/service/**");
        expectZuulRoute.setServiceId("service");
        LinkedHashMap<String, ZuulProperties.ZuulRoute> expectedRoutes = new LinkedHashMap();
        expectedRoutes.put("/prefix/prefix/service/**", expectZuulRoute);

        ZuulProperties.ZuulRoute expectZuulRoute2 = new ZuulProperties.ZuulRoute();
        expectZuulRoute2.setId("api/v1/service");
        expectZuulRoute2.setPath("/api/v1/service/**");
        expectZuulRoute2.setServiceId("service");
        LinkedHashMap<String, ZuulProperties.ZuulRoute> expectedRoutes2 = new LinkedHashMap();
        expectedRoutes2.put("/prefix/api/v1/service/**", expectZuulRoute2);

        assertEquals(expectedRoutes.get("/prefix/prefix/service/**"), routes.get("/prefix/prefix/service/**"));
        assertEquals(expectedRoutes2.get("/prefix/api/v1/service/**"), routes.get("/prefix/api/v1/service/**"));
    }

    @Test
    public void shouldAddRoutedService_WhenLocateRoutes_IfRoutedServiceExists() {
        routedServicesUsers.add(locationFilter);

        apimlRouteLocator = new ApimlRouteLocator("service", discovery, properties, serviceRouteMapper, routedServicesUsers);

        Map<String, String> metadata = new HashMap<>();
        metadata.put(ROUTES + ".api-v1." + ROUTES_GATEWAY_URL, "api/v1");
        metadata.put(ROUTES + ".api-v1." + ROUTES_SERVICE_URL, "api");

        when(discovery.getServices()).thenReturn(Collections.singletonList("service"));
        when(discovery.getInstances("service")).thenReturn(
            Collections.singletonList(new DefaultServiceInstance("service", "localhost", 80, false, metadata)));
        when(serviceRouteMapper.apply("service")).thenReturn("service");

        LinkedHashMap<String, ZuulProperties.ZuulRoute> routes = apimlRouteLocator.locateRoutes();

        ZuulProperties.ZuulRoute expectZuulRoute = new ZuulProperties.ZuulRoute();
        expectZuulRoute.setId("api/v1/service");
        expectZuulRoute.setPath("/api/v1/service/**");
        expectZuulRoute.setServiceId("service");
        LinkedHashMap<String, ZuulProperties.ZuulRoute> expectedRoutes = new LinkedHashMap();
        expectedRoutes.put("/api/v1/service/**", expectZuulRoute);

        assertEquals(expectedRoutes, routes);
    }

}
