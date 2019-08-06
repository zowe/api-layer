/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.metadata;

import com.ca.mfaas.eurekaservice.model.ApiInfo;
import com.ca.mfaas.product.routing.RoutedService;
import com.ca.mfaas.product.routing.RoutedServices;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class EurekaMetadataParserTest {

    @Test
    public void testParseApiInfo() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("apiml.apiInfo.1.gatewayUrl", "gatewayUrl");
        metadata.put("apiml.apiInfo.2.gatewayUrl", "gatewayUrl2");
        metadata.put("apiml.apiInfo.2.swaggerUrl", "swagger");
        metadata.put("apiml.apiInfo.2.documentationUrl", "doc");

        List<ApiInfo> info = new EurekaMetadataParser().parseApiInfo(metadata);
        assertEquals(2, info.size());
        assertEquals("gatewayUrl", info.get(0).getGatewayUrl());
        assertEquals("gatewayUrl2", info.get(1).getGatewayUrl());
        assertEquals("swagger", info.get(1).getSwaggerUrl());
        assertEquals("doc", info.get(1).getDocumentationUrl());
    }

    @Test
    public void testParseRoutes() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("routes.api-v1.gateway-url", "api/v1");
        metadata.put("routes.api-v1.service-url", "/");
        metadata.put("other.parameter", "value");
        metadata.put("routes.api-v2.service-url", "/test");
        metadata.put("some.garbage.again", "null");
        metadata.put("routes.api-v2.gateway-url", "api/v2");
        metadata.put("routes.api-v3.gateway-url", "incomplete");
        metadata.put("routes.api-v4.service-url", "incomplete");
        metadata.put("routes.api-v5.gateway-url", "/api/v5/");
        metadata.put("routes.api-v5.service-url", "test");

        RoutedServices routes = new EurekaMetadataParser().parseRoutes(metadata);

        RoutedServices expectedRoutes = new RoutedServices();
        expectedRoutes.addRoutedService(
            new RoutedService("api-v1", "api/v1", "/"));
        expectedRoutes.addRoutedService(
            new RoutedService("api-v2", "api/v2", "/test"));
        expectedRoutes.addRoutedService(
            new RoutedService("api-v5", "api/v5", "/test"));

        assertEquals(expectedRoutes.toString(), routes.toString());
    }
}
