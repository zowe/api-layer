/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.product.routing;


import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
    public void testBestMatchingServiceUrlByAllServiceTypes() {
        RoutedService routedService = routedServices.getBestMatchingServiceUrl("/apicatalog", ServiceType.ALL);
        assertNotNull(routedService);

        assertEquals("api_v1", routedService.getSubServiceId());
        assertEquals("api/v1", routedService.getGatewayUrl());
        assertEquals("/apicatalog", routedService.getServiceUrl());


        routedServices.addRoutedService(new RoutedService("api_v1", "api/v1", "/apicatalog"));
        routedServices.addRoutedService(new RoutedService("ui_v1", "ui/v1", "/apicatalog/test"));


        routedService = routedServices.getBestMatchingServiceUrl("/apicatalog/test/param1", ServiceType.ALL);
        assertNotNull(routedService);

        assertEquals("ui_v1", routedService.getSubServiceId());
        assertEquals("ui/v1", routedService.getGatewayUrl());
        assertEquals("/apicatalog/test", routedService.getServiceUrl());
    }

    @Test
    public void testBestMatchingServiceUrlBySpecificServiceTypes() {
        RoutedService routedService = routedServices.getBestMatchingServiceUrl("/apicatalog", ServiceType.API);

        assertEquals("api_v1", routedService.getSubServiceId());
        assertEquals("api/v1", routedService.getGatewayUrl());
        assertEquals("/apicatalog", routedService.getServiceUrl());

        RoutedService routedService3 = new RoutedService("api_v2", "api/v2", "/apicatalog2");
        RoutedService routedService4 = new RoutedService("ui_v2", "ui/v2", "/apicatalog2");
        routedServices.addRoutedService(routedService3);
        routedServices.addRoutedService(routedService4);

        routedService = routedServices.getBestMatchingServiceUrl("/apicatalog2", ServiceType.API);

        assertEquals("api_v2", routedService.getSubServiceId());
        assertEquals("api/v2", routedService.getGatewayUrl());
        assertEquals("/apicatalog2", routedService.getServiceUrl());


        routedService = routedServices.getBestMatchingServiceUrl("/apicatalog2", ServiceType.UI);

        assertEquals("ui_v2", routedService.getSubServiceId());
        assertEquals("ui/v2", routedService.getGatewayUrl());
        assertEquals("/apicatalog2", routedService.getServiceUrl());
    }

    @Test
    public void testBestMatchingServiceUrlWithRouteServiceUrlStartingWithServiceUrl(){
        RoutedService routedService1 = new RoutedService("api_v2", "api/v2", "/apicatalog/api/v2");
        RoutedService routedService2 = new RoutedService("ui_v2", "ui/v2", "/apicatalog/ui/v2");
        routedServices.addRoutedService(routedService1);
        routedServices.addRoutedService(routedService2);

        RoutedService routedService = routedServices.getBestMatchingServiceUrl("/apicatalog", ServiceType.UI);

        assertEquals("ui_v2", routedService.getSubServiceId());
        assertEquals("ui/v2", routedService.getGatewayUrl());
        assertEquals("/apicatalog/ui/v2", routedService.getServiceUrl());
    }
}
