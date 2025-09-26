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
import org.zowe.apiml.product.instance.ServiceAddress;
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
    private TransformService transformService;

    @BeforeEach
    void setup() {
        ServiceAddress gatewayConfig = ServiceAddress.builder()
            .scheme("https")
            .hostname("localhost")
            .build();
        gatewayClient = new GatewayClient(gatewayConfig);
        transformService = new TransformService(gatewayClient);
    }

    private RoutedServices buildRoutedServices(RoutedService... routes) {
        RoutedServices routedServices = new RoutedServices();
        for (RoutedService route : routes) {
            routedServices.addRoutedService(route);
        }
        return routedServices;
    }

    private String expectedUrl(String serviceId, String prefix, String path) {
        return String.format("%s://%s/%s/%s%s",
            gatewayClient.getGatewayConfigProperties().getScheme(),
            gatewayClient.getGatewayConfigProperties().getHostname(),
            serviceId,
            prefix,
            path == null ? "" : path
        );
    }

    @ParameterizedTest
    @CsvSource({
        "https://localhost:8080/ui,UI,ui,''",
        "https://localhost:8080/ws,WS,ws,''",
        "https://localhost:8080/api,API,api,''",
        "https://locahost:8080/ui/service/login.do?action=secure,UI,ui,'/login.do?action=secure'"
    })
    void givenValidRoutes_whenTransform_thenReturnExpectedUrl(
        String url, ServiceType type, String prefix, String extraPath
    ) throws URLTransformationException {
        RoutedServices routes = buildRoutedServices(
            new RoutedService(SERVICE_ID, prefix, url.contains("/ui/service") ? "/ui/service" : "/" + prefix),
            new RoutedService(SERVICE_ID, "api/v1", "/")
        );

        String actual = transformService.transformURL(type, SERVICE_ID, url, routes, false);
        assertEquals(expectedUrl(SERVICE_ID, prefix, extraPath), actual);
    }

    @ParameterizedTest
    @CsvSource({
        "https://localhost:8080/,WS,ws,'/'",
        "https://locahost:8080/test,UI,ui,'/test'",
    })
    void givenMissingRoutes_whenTransform_thenThrow(
        String url, ServiceType type, String prefix, String extraPath
    ) {
        RoutedServices routes = buildRoutedServices(
            new RoutedService(SERVICE_ID, prefix, url.contains("/ui/service") ? "/ui/service" : "/" + prefix),
            new RoutedService(SERVICE_ID, "api/v1", "/")
        );
        assertThrows(URLTransformationException.class, () -> transformService.transformURL(type, SERVICE_ID, url, routes, false));
    }

    @Test
    void givenHttpUrlAndAttlsEnabled_whenTransform_thenForceHttps() throws URLTransformationException {
        String url = "http://localhost:8080/ui";
        RoutedServices routes = buildRoutedServices(
            new RoutedService(SERVICE_ID, UI_PREFIX, "/ui"),
            new RoutedService(SERVICE_ID, "api/v1", "/")
        );

        String actual = transformService.transformURL(ServiceType.UI, SERVICE_ID, url, routes, true);

        assertEquals("https://localhost/service/ui", actual);
    }

    @Test
    void givenRouteNotFound_whenTransform_thenThrowException() {
        String url = "https://localhost:8080/u";
        RoutedServices routes = buildRoutedServices(
            new RoutedService(SERVICE_ID, UI_PREFIX, "/ui"),
            new RoutedService(SERVICE_ID, "api/v1", "/")
        );

        URLTransformationException ex = assertThrows(URLTransformationException.class,
            () -> transformService.transformURL(ServiceType.UI, SERVICE_ID, url, routes, false));

        assertEquals("Not able to select route for url https://localhost:8080/u of the service service. Original url used.",
            ex.getMessage());
    }

    @Test
    void givenInvalidUrl_whenTransform_thenThrowException() {
        String url = "https:localhost:8080/wss";
        TransformService service = new TransformService(gatewayClient);

        URLTransformationException ex = assertThrows(URLTransformationException.class,
            () -> service.transformURL(null, null, url, null, false));

        assertEquals("The URI " + url + " is not valid.", ex.getMessage());
    }

    @Test
    void givenEmptyGatewayClient_whenTransform_thenThrowException() {
        GatewayClient emptyClient = new GatewayClient(null);
        TransformService service = new TransformService(emptyClient);

        URLTransformationException ex = assertThrows(URLTransformationException.class,
            () -> service.transformURL(null, null, "https://localhost:8080/wss", null, false));

        assertEquals("Gateway not found yet, transform service cannot perform the request", ex.getMessage());
    }

    @Test
    void givenInvalidPath_whenTransform_thenThrowException() {
        String url = "https://localhost:8080/wss";
        RoutedServices routes = buildRoutedServices(
            new RoutedService(SERVICE_ID, WS_PREFIX, "/ws"),
            new RoutedService(SERVICE_ID, "api/v1", "/")
        );

        URLTransformationException ex = assertThrows(URLTransformationException.class,
            () -> transformService.transformURL(ServiceType.WS, SERVICE_ID, url, routes, false));

        assertEquals("The path /wss of the service URL https://localhost:8080/wss is not valid.", ex.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
        // url, routePath, expected
        "https://localhost:8080/service/api/v1,/service/api,/service/api/{api-version}",
        "https://localhost:8080/service/api/v1,/service/api/v1,/service/api/{api-version}"
    })
    void givenApiRoute_whenRetrieveApiBasePath_thenReturnExpected(
        String url, String routePath, String expected
    ) throws URLTransformationException {
        RoutedServices routes = buildRoutedServices(
            new RoutedService(SERVICE_ID, API_PREFIX + "/v1", routePath)
        );

        TransformService service = new TransformService(null);
        String actual = service.retrieveApiBasePath(SERVICE_ID, url, routes);

        assertEquals(expected, actual);
    }

    @Test
    void givenInvalidUrl_whenRetrieveApiBasePath_thenThrowException() {
        String url = "https:localhost:8080/wss";
        TransformService service = new TransformService(null);

        URLTransformationException ex = assertThrows(URLTransformationException.class,
            () -> service.retrieveApiBasePath(null, url, null));

        assertEquals("The URI " + url + " is not valid.", ex.getMessage());
    }

    @Test
    void givenNoMatchingRoute_whenRetrieveApiBasePath_thenThrowException() {
        String url = "https://localhost:8080/u";
        RoutedServices routes = buildRoutedServices(
            new RoutedService(SERVICE_ID, UI_PREFIX, "/ui"),
            new RoutedService(SERVICE_ID, "api/v1", "/api")
        );

        TransformService service = new TransformService(null);

        URLTransformationException ex = assertThrows(URLTransformationException.class,
            () -> service.retrieveApiBasePath(SERVICE_ID, url, routes));

        assertEquals("Not able to select API base path for the service " + SERVICE_ID + ". Original url used.",
            ex.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
        "service,api/v1,api/v1,api/v1,/service/api/{api-version}",
        "srv,api/v1/home/page.html,api/v1,api/v1,/srv/api/{api-version}",
        "srv,wrong/url,api/v1,api/v1,",
        "srv,apiV1/home/page.html,api/v1,apiV1,/srv/api/{api-version}"
    })
    void testRetrieveApiBasePathParameterized(
        String serviceId, String url, String gatewayUrl, String serviceUrl, String expectedBasePath
    ) {
        RoutedServices routes = buildRoutedServices(
            new RoutedService("api", gatewayUrl, serviceUrl)
        );

        TransformService service = new TransformService(null);
        String basePath;
        try {
            basePath = service.retrieveApiBasePath(serviceId, url, routes);
        } catch (URLTransformationException e) {
            basePath = null;
        }

        assertEquals(expectedBasePath, basePath);
    }

    @Test
    void givenLocationUriNotMatchingServiceRoutes_whenTransformAbsoluteURL_thenThrowException() {

        RoutedService route = new RoutedService("dcpassticket", "api", "/api");

        var ex = assertThrows(URLTransformationException.class, () ->
            transformService.transformAbsoluteURL("dcpassticket", "/apisome-path", route));

        assertEquals("The path /apisome-path of the service dcpassticket is not valid.", ex.getMessage());
    }

    @Test
    void givenMatchingLocation_whenTransformAbsoluteURL_thenReturnAbsoluteURL() throws URLTransformationException {

        RoutedService route = new RoutedService("dcpassticket", "api", "/api");

        var result = transformService.transformAbsoluteURL("dcpassticket", "/api/v1/some-path", route);
        assertEquals("/dcpassticket/api/v1/some-path", result);
    }

}
