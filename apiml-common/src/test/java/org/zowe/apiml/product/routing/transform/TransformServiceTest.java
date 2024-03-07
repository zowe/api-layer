/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.routing.transform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.gateway.GatewayConfigProperties;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.product.routing.RoutedServices;
import org.zowe.apiml.product.routing.ServiceType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransformServiceTest {

    private static final String UI_PREFIX = "ui";
    private static final String API_PREFIX = "api";
    private static final String WS_PREFIX = "ws";

    private static final String SERVICE_ID = "service";

    private GatewayClient gatewayClient;

    @BeforeEach
    void setup() {
        GatewayConfigProperties gatewayConfigProperties = GatewayConfigProperties.builder()
            .scheme("https")
            .hostname("localhost")
            .build();
        gatewayClient = new GatewayClient(gatewayConfigProperties);
    }

    @Test
    void givenHomePageAndUIRoute_whenTransform_thenUseNewUrl() throws URLTransformationException {
        String url = "https://localhost:8080/ui";

        RoutedServices routedServices = new RoutedServices();
        RoutedService routedService1 = new RoutedService(SERVICE_ID, UI_PREFIX, "/ui");
        RoutedService routedService2 = new RoutedService(SERVICE_ID, "api/v1", "/");
        routedServices.addRoutedService(routedService1);
        routedServices.addRoutedService(routedService2);

        TransformService transformService = new TransformService(gatewayClient);
        String actualUrl = transformService.transformURL(ServiceType.UI, SERVICE_ID, url, routedServices, false);

        String expectedUrl = String.format("%s://%s/%s/%s",
            gatewayClient.getGatewayConfigProperties().getScheme(),
            gatewayClient.getGatewayConfigProperties().getHostname(),
            SERVICE_ID,
            UI_PREFIX);
        assertEquals(expectedUrl, actualUrl);
    }

    @Test
    void givenHomePageWithAttlsEnabled_whenTransform_thenUseNewUrlWithHttps() throws URLTransformationException {
        String url = "http://localhost:8080/ui";
        RoutedServices routedServices = new RoutedServices();
        RoutedService routedService1 = new RoutedService(SERVICE_ID, UI_PREFIX, "/ui");
        RoutedService routedService2 = new RoutedService(SERVICE_ID, "api/v1", "/");
        routedServices.addRoutedService(routedService1);
        routedServices.addRoutedService(routedService2);

        TransformService transformService = new TransformService(gatewayClient);
        String actualUrl = transformService.transformURL(ServiceType.UI, SERVICE_ID, url, routedServices, true);

        String expectedUrl = String.format("%s://%s/%s/%s",
            "https",
            gatewayClient.getGatewayConfigProperties().getHostname(),
            SERVICE_ID,
            UI_PREFIX);
        assertEquals(expectedUrl, actualUrl);
    }

    @Test
    void givenHomePage_whenRouteNotFound_thenThrowException() {
        String url = "https://localhost:8080/u";

        RoutedServices routedServices = new RoutedServices();
        RoutedService routedService1 = new RoutedService(SERVICE_ID, UI_PREFIX, "/ui");
        RoutedService routedService2 = new RoutedService(SERVICE_ID, "api/v1", "/");
        routedServices.addRoutedService(routedService1);
        routedServices.addRoutedService(routedService2);

        TransformService transformService = new TransformService(gatewayClient);

        Exception exception = assertThrows(URLTransformationException.class, () -> {
            transformService.transformURL(ServiceType.UI, SERVICE_ID, url, routedServices, false);
        });
        assertEquals("Not able to select route for url https://localhost:8080/u of the service service. Original url used.", exception.getMessage());
    }

    @Test
    void givenHomePageAndWSRoute_whenTransform_thenUseNewUrl() throws URLTransformationException {
        String url = "https://localhost:8080/ws";

        RoutedServices routedServices = new RoutedServices();
        RoutedService routedService1 = new RoutedService(SERVICE_ID, WS_PREFIX, "/ws");
        RoutedService routedService2 = new RoutedService(SERVICE_ID, "api/v1", "/");
        routedServices.addRoutedService(routedService1);
        routedServices.addRoutedService(routedService2);

        TransformService transformService = new TransformService(gatewayClient);
        String actualUrl = transformService.transformURL(ServiceType.WS, SERVICE_ID, url, routedServices, false);

        String expectedUrl = String.format("%s://%s/%s/%s",
            gatewayClient.getGatewayConfigProperties().getScheme(),
            gatewayClient.getGatewayConfigProperties().getHostname(),
            SERVICE_ID,
            WS_PREFIX);
        assertEquals(expectedUrl, actualUrl);
    }

    @Test
    void givenHomePageAndAPIRoute_whenTransform_thenUseNewUrl() throws URLTransformationException {
        String url = "https://localhost:8080/api";

        RoutedServices routedServices = new RoutedServices();
        RoutedService routedService1 = new RoutedService(SERVICE_ID, API_PREFIX, "/api");
        RoutedService routedService2 = new RoutedService(SERVICE_ID, "api/v1", "/");
        routedServices.addRoutedService(routedService1);
        routedServices.addRoutedService(routedService2);

        TransformService transformService = new TransformService(gatewayClient);
        String actualUrl = transformService.transformURL(ServiceType.API, SERVICE_ID, url, routedServices, false);

        String expectedUrl = String.format("%s://%s/%s/%s",
            gatewayClient.getGatewayConfigProperties().getScheme(),
            gatewayClient.getGatewayConfigProperties().getHostname(),
            SERVICE_ID,
            API_PREFIX);
        assertEquals(expectedUrl, actualUrl);
    }


    @Test
    void givenInvalidHomePage_thenThrowException() {
        String url = "https:localhost:8080/wss";

        TransformService transformService = new TransformService(gatewayClient);

        Exception exception = assertThrows(URLTransformationException.class, () -> {
            transformService.transformURL(null, null, url, null, false);
        });
        assertEquals("The URI " + url + " is not valid.", exception.getMessage());
    }

    @Test
    void givenEmptyGatewayClient_thenThrowException() {
        String url = "https:localhost:8080/wss";

        GatewayClient emptyGatewayClient = new GatewayClient(null);
        TransformService transformService = new TransformService(emptyGatewayClient);

        Exception exception = assertThrows(URLTransformationException.class, () -> {
            transformService.transformURL(null, null, url, null, false);
        });
        assertEquals("Gateway not found yet, transform service cannot perform the request", exception.getMessage());
    }


    @Test
    void givenHomePage_whenPathIsNotValid_thenThrowException() {
        String url = "https://localhost:8080/wss";

        RoutedServices routedServices = new RoutedServices();
        RoutedService routedService1 = new RoutedService(SERVICE_ID, WS_PREFIX, "/ws");
        RoutedService routedService2 = new RoutedService(SERVICE_ID, "api/v1", "/");
        routedServices.addRoutedService(routedService1);
        routedServices.addRoutedService(routedService2);

        TransformService transformService = new TransformService(gatewayClient);

        Exception exception = assertThrows(URLTransformationException.class, () -> {
            transformService.transformURL(ServiceType.WS, SERVICE_ID, url, routedServices, false);
        });
        assertEquals("The path /wss of the service URL https://localhost:8080/wss is not valid.", exception.getMessage());
    }

    @Test
    void givenEmptyPathInHomePage_whenTransform_thenUseNewUrl() throws URLTransformationException {
        String url = "https://localhost:8080/";

        RoutedServices routedServices = new RoutedServices();
        RoutedService routedService1 = new RoutedService(SERVICE_ID, WS_PREFIX, "/");
        RoutedService routedService2 = new RoutedService(SERVICE_ID, "api/v1", "/");
        routedServices.addRoutedService(routedService1);
        routedServices.addRoutedService(routedService2);

        TransformService transformService = new TransformService(gatewayClient);

        String actualUrl = transformService.transformURL(ServiceType.WS, SERVICE_ID, url, routedServices, false);
        String expectedUrl = String.format("%s://%s/%s/%s%s",
            gatewayClient.getGatewayConfigProperties().getScheme(),
            gatewayClient.getGatewayConfigProperties().getHostname(),
            SERVICE_ID,
            WS_PREFIX,
            "/");
        assertEquals(expectedUrl, actualUrl);
    }

    @Test
    void givenServiceUrl_whenItsRoot_thenKeepHomePagePathSame() throws URLTransformationException {
        String url = "https://locahost:8080/test";
        RoutedServices routedServices = new RoutedServices();
        RoutedService routedService1 = new RoutedService(SERVICE_ID, UI_PREFIX, "/");
        RoutedService routedService2 = new RoutedService(SERVICE_ID, "api/v1", "/");
        routedServices.addRoutedService(routedService1);
        routedServices.addRoutedService(routedService2);

        TransformService transformService = new TransformService(gatewayClient);

        String actualUrl = transformService.transformURL(ServiceType.UI, SERVICE_ID, url, routedServices, false);
        String expectedUrl = String.format("%s://%s/%s/%s%s",
            gatewayClient.getGatewayConfigProperties().getScheme(),
            gatewayClient.getGatewayConfigProperties().getHostname(),
            SERVICE_ID,
            UI_PREFIX,
            "/test");
        assertEquals(expectedUrl, actualUrl);
    }

    @Test
    void givenUrlContainingPathAndQuery_whenTransform_thenKeepQueryPartInTheNewUrl() throws URLTransformationException {
        String url = "https://locahost:8080/ui/service/login.do?action=secure";
        String path = "/login.do?action=secure";
        RoutedServices routedServices = new RoutedServices();
        RoutedService routedService1 = new RoutedService(SERVICE_ID, UI_PREFIX, "/ui/service");
        RoutedService routedService2 = new RoutedService(SERVICE_ID, "api/v1", "/");
        routedServices.addRoutedService(routedService1);
        routedServices.addRoutedService(routedService2);

        TransformService transformService = new TransformService(gatewayClient);

        String actualUrl = transformService.transformURL(ServiceType.UI, SERVICE_ID, url, routedServices, false);
        String expectedUrl = String.format("%s://%s/%s/%s%s",
            gatewayClient.getGatewayConfigProperties().getScheme(),
            gatewayClient.getGatewayConfigProperties().getHostname(),
            SERVICE_ID,
            UI_PREFIX,
            path);
        assertEquals(expectedUrl, actualUrl);
    }

    @Test
    void givenServiceAndApiRoute_whenGetApiBasePath_thenReturnApiPath() throws URLTransformationException {
        String url = "https://localhost:8080/" + SERVICE_ID + "/" + API_PREFIX + "/v1";

        String serviceUrl = String.format("/%s/%s", SERVICE_ID, API_PREFIX);
        RoutedServices routedServices = new RoutedServices();
        RoutedService routedService = new RoutedService(SERVICE_ID, API_PREFIX, serviceUrl);
        routedServices.addRoutedService(routedService);

        TransformService transformService = new TransformService(null);

        String actualPath = transformService.retrieveApiBasePath(SERVICE_ID, url, routedServices);
        String expectedPath = String.format("/%s/%s",
            SERVICE_ID,
            API_PREFIX);
        assertEquals(expectedPath, actualPath);
    }

    @Test
    void givenServiceAndApiRouteWithVersion_whenGetApiBasePath_thenReturnApiPath() throws URLTransformationException {
        String url = "https://localhost:8080/" + SERVICE_ID + "/" + API_PREFIX + "/v1";

        String serviceUrl = String.format("/%s/%s/%s", SERVICE_ID, API_PREFIX, "v1");
        RoutedServices routedServices = new RoutedServices();
        RoutedService routedService = new RoutedService(SERVICE_ID, API_PREFIX + "/v1", serviceUrl);
        routedServices.addRoutedService(routedService);

        TransformService transformService = new TransformService(null);

        String actualPath = transformService.retrieveApiBasePath(SERVICE_ID, url, routedServices);
        String expectedPath = String.format("/%s/%s/%s", SERVICE_ID, API_PREFIX, "{api-version}");
        assertEquals(expectedPath, actualPath);
    }

    @Test
    void givenInvalidUriPath_whenGetApiBasePath_thenThrowError() {
        String url = "https:localhost:8080/wss";

        TransformService transformService = new TransformService(null);

        Exception exception = assertThrows(URLTransformationException.class, () -> {
            transformService.retrieveApiBasePath(null, url, null);
        });
        assertEquals("The URI " + url + " is not valid.", exception.getMessage());
    }

    @Test
    void givenNoRoutes_whenGetApiBasePath_thenThrowError() {
        String url = "https://localhost:8080/u";

        RoutedServices routedServices = new RoutedServices();
        RoutedService routedService1 = new RoutedService(SERVICE_ID, UI_PREFIX, "/ui");
        RoutedService routedService2 = new RoutedService(SERVICE_ID, "api/v1", "/api");
        routedServices.addRoutedService(routedService1);
        routedServices.addRoutedService(routedService2);

        TransformService transformService = new TransformService(null);

        Exception exception = assertThrows(URLTransformationException.class, () -> {
            transformService.retrieveApiBasePath(SERVICE_ID, url, routedServices);
        });
        assertEquals("Not able to select API base path for the service " + SERVICE_ID + ". Original url used.", exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
        "service,api/v1,api/v1,api/v1,/service/api/{api-version}",
        "srv,api/v1/home/page.html,api/v1,api/v1,/srv/api/{api-version}",
        "srv,wrong/url,api/v1,api/v1,",
        "srv,apiV1/home/page.html,api/v1,apiV1,/srv/api/{api-version}"
    })
    void testRetrieveApiBasePath(String serviceId, String url, String gatewayUrl, String serviceUrl, String expectedBasePath) {
        RoutedService route = new RoutedService("api", gatewayUrl, serviceUrl);

        RoutedServices routedServices = new RoutedServices();
        routedServices.addRoutedService(route);

        TransformService transformService = new TransformService(null);
        String basePath;
        try {
            basePath = transformService.retrieveApiBasePath(serviceId, url, routedServices);
        } catch (URLTransformationException e) {
            basePath = null;
        }

        assertEquals(expectedBasePath, basePath);
    }

}
