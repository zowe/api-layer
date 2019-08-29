/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.eurekaservice.client.util;

import com.ca.mfaas.config.ApiInfo;
import com.ca.mfaas.product.routing.RoutedService;
import com.ca.mfaas.product.routing.RoutedServices;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ca.mfaas.constants.EurekaMetadataDefinition.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;

public class EurekaMetadataParserTest {

    private final EurekaMetadataParser eurekaMetadataParser = new EurekaMetadataParser();

    @Test
    public void testParseApiInfo() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(API_INFO + ".1." + API_INFO_GATEWAY_URL, "gatewayUrl");
        metadata.put(API_INFO + ".2." + API_INFO_GATEWAY_URL, "gatewayUrl2");
        metadata.put(API_INFO + ".2." + API_INFO_SWAGGER_URL, "swagger");
        metadata.put(API_INFO + ".2." + API_INFO_DOCUMENTATION_URL, "doc");

        List<ApiInfo> info = eurekaMetadataParser.parseApiInfo(metadata);
        assertEquals(2, info.size());
        assertEquals("gatewayUrl", info.get(0).getGatewayUrl());
        assertEquals("gatewayUrl2", info.get(1).getGatewayUrl());
        assertEquals("swagger", info.get(1).getSwaggerUrl());
        assertEquals("doc", info.get(1).getDocumentationUrl());
    }

    @Test
    public void testParseRoutes() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(ROUTES + ".api-v1." + ROUTES_GATEWAY_URL, "api/v1");
        metadata.put(ROUTES + ".api-v1." + ROUTES_SERVICE_URL, "/");
        metadata.put("other.parameter", "value");
        metadata.put(ROUTES + ".api-v2." + ROUTES_SERVICE_URL, "/test");
        metadata.put("some.garbage.again", "null");
        metadata.put(ROUTES + ".api-v2." + ROUTES_GATEWAY_URL, "api/v2");
        metadata.put(ROUTES + ".api-v3." + ROUTES_GATEWAY_URL, "incomplete");
        metadata.put(ROUTES + ".api-v4." + ROUTES_SERVICE_URL, "incomplete");
        metadata.put(ROUTES + ".api-v5." + ROUTES_GATEWAY_URL, "/api/v5/");
        metadata.put(ROUTES + ".api-v5." + ROUTES_SERVICE_URL, "test");

        RoutedServices routes = eurekaMetadataParser.parseRoutes(metadata);

        RoutedServices expectedRoutes = new RoutedServices();
        expectedRoutes.addRoutedService(
            new RoutedService("api-v1", "api/v1", "/"));
        expectedRoutes.addRoutedService(
            new RoutedService("api-v2", "api/v2", "/test"));
        expectedRoutes.addRoutedService(
            new RoutedService("api-v5", "api/v5", "/test"));

        assertEquals(expectedRoutes.toString(), routes.toString());
    }


    @Test
    public void testParseToListRoute() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(ROUTES + ".api-v1." + ROUTES_GATEWAY_URL, "api/v1");
        metadata.put(ROUTES + ".api-v1." + ROUTES_SERVICE_URL, "/");
        metadata.put("other.parameter", "value");
        metadata.put(ROUTES + ".api-v2." + ROUTES_SERVICE_URL, "/test");
        metadata.put("some.garbage.again", "null");
        metadata.put(ROUTES + ".api-v2." + ROUTES_GATEWAY_URL, "api/v2");
        metadata.put(ROUTES + ".api-v3." + ROUTES_GATEWAY_URL, "incomplete");
        metadata.put(ROUTES + ".api-v4." + ROUTES_SERVICE_URL, "incomplete");
        metadata.put(ROUTES + ".api-v5." + ROUTES_GATEWAY_URL, "/api/v5/");
        metadata.put(ROUTES + ".api-v5." + ROUTES_SERVICE_URL, "test");

        List<RoutedService> actualRoutes = eurekaMetadataParser.parseToListRoute(metadata);
        List<RoutedService> expectedListRoute = Arrays.asList(
            new RoutedService("api-v1", "api/v1", "/"),
            new RoutedService("api-v2", "api/v2", "/test"),
            new RoutedService("api-v5", "api/v5", "/test")
        );

        assertEquals("List route size is different", 3, actualRoutes.size());
        assertThat(actualRoutes, containsInAnyOrder(expectedListRoute.toArray()));

    }
}
