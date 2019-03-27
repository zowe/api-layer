package com.ca.mfaas.product.routing;


import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RoutedServicesTest {

    private RoutedServices routedServices;

    @Before
    public void setup() {
        routedServices = new RoutedServices();
        RoutedService routedService = new RoutedService("api_v1", "api/v1", "/apicatalog");
        RoutedService routedService2 = new RoutedService("ui_v1", "ui/v1", "/apicatalog");

        routedServices.addRoutedService(routedService);
        routedServices.addRoutedService(routedService2);

    }

    @Test
    public void testFindServiceByGatewayUrl() {
        RoutedService routedServiceApiV1 = routedServices.findServiceByGatewayUrl("api/v1");

        assertEquals("api_v1", routedServiceApiV1.getSubServiceId());
        assertEquals("api/v1", routedServiceApiV1.getGatewayUrl());
        assertEquals("/apicatalog", routedServiceApiV1.getServiceUrl());

        RoutedService routedServiceUIV1 = routedServices.findServiceByGatewayUrl("ui/v1");

        assertEquals("ui_v1", routedServiceUIV1.getSubServiceId());
        assertEquals("ui/v1", routedServiceUIV1.getGatewayUrl());
        assertEquals("/apicatalog", routedServiceUIV1.getServiceUrl());
    }

    @Test
    public void testNotFoundBestMatchingServiceUrl() {
        RoutedService routedService = routedServices.getBestMatchingServiceUrl("/apicatalo", true);
        assertNull(routedService);
    }

    @Test
    public void testBestMatchingServiceUrl() {
        RoutedService routedService = routedServices.getBestMatchingServiceUrl("/apicatalog", true);

        assertEquals("api_v1", routedService.getSubServiceId());
        assertEquals("api/v1", routedService.getGatewayUrl());
        assertEquals("/apicatalog", routedService.getServiceUrl());

        RoutedService routedService3 = new RoutedService("api_v2", "api/v2", "/apicatalog2");
        RoutedService routedService4 = new RoutedService("ui_v2", "ui/v2", "/apicatalog2");
        routedServices.addRoutedService(routedService3);
        routedServices.addRoutedService(routedService4);

        routedService = routedServices.getBestMatchingServiceUrl("/apicatalog2", true);

        assertEquals("api_v2", routedService.getSubServiceId());
        assertEquals("api/v2", routedService.getGatewayUrl());
        assertEquals("/apicatalog2", routedService.getServiceUrl());
    }

    @Test
    public void testBestMatchingServiceUrlForNotOnlyApi() {
        RoutedService routedService3 = new RoutedService("api_v2", "api/v2", "/apicatalog");
        RoutedService routedService4 = new RoutedService("ui_v2", "ui/v2", "/apicatalog2");
        routedServices.addRoutedService(routedService3);
        routedServices.addRoutedService(routedService4);

        RoutedService routedService = routedServices.getBestMatchingServiceUrl("/apicatalog2", false);

        assertEquals("ui_v2", routedService.getSubServiceId());
        assertEquals("ui/v2", routedService.getGatewayUrl());
        assertEquals("/apicatalog2", routedService.getServiceUrl());
    }
}
