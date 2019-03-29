/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.product.routing;

import com.ca.mfaas.product.gateway.GatewayConfigProperties;
import org.junit.Test;

import static org.junit.Assert.*;

public class TransformServiceTest {

    @Test
    public void transformURL() {
        //String url = "httrps://localhost:8080/ui";
        String url = "httfbdffvx";
        GatewayConfigProperties gateway = GatewayConfigProperties.builder()
            .scheme("https")
            .hostname("gwhost")
            .build();
        String prefix = "u";
        String serviceId = "service";

        RoutedServices routedServices = new RoutedServices();
        RoutedService routedService1 = new RoutedService(serviceId, prefix, "/ui");
        RoutedService routedService2 = new RoutedService(serviceId, "api/v1", "/");
        routedServices.addRoutedService(routedService1);
        routedServices.addRoutedService(routedService2);

        TransformService transformService = new TransformService(gateway);
        String actualUrl = transformService.transformURL(ServiceType.UI, serviceId, url, routedServices);

        String expectedUrl = String.format("%s://%s/%s/%s",
            gateway.getScheme(),
            gateway.getHostname(),
            prefix,
            serviceId);
        assertEquals(expectedUrl, actualUrl);
    }
}
