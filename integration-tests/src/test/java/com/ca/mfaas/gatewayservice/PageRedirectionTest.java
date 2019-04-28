/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gatewayservice;

import com.ca.mfaas.utils.config.ConfigReader;
import com.ca.mfaas.utils.config.DiscoverableClientConfiguration;
import com.ca.mfaas.utils.config.GatewayServiceConfiguration;
import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.core.Is.is;
import static org.springframework.http.HttpHeaders.LOCATION;

/**
 * This is an integration test class for PageRedirectionFilter.java
 * It tests PageRedirectionFilter with service staticclient which is defined staticclient.yml
 * <ol>
 * The test scenario is:
 * <li>Send POST request to PageRedirectionController, the request body contains a service url</li>
 * <li>When PageRedirectionController gets the POST request, it takes out the url from request body, sets Location
 * Response header to the url, and returns status code 307</li>
 * <li>The response should then be filtered by PageRedirectionFilter</li>
 * <li>Checks the Location in response header, verify that it has been tranformed to gateway url</li>
 * </ol>
 */
public class PageRedirectionTest {
    private final String serviceId = "staticclient";
    private final String baseUrl = "/discoverableclient";
    private final String apiPrefix = "/api/v1";
    private String gatewayScheme;
    private String gatewayHost;
    private int gatewayPort;
    private String dcScheme;
    private String dcHost;
    private int dcPort;
    private String requestUrl;

    @Before
    public void setUp() {
        GatewayServiceConfiguration gatewayServiceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        gatewayScheme = gatewayServiceConfiguration.getScheme();
        gatewayHost = gatewayServiceConfiguration.getHost();
        gatewayPort = gatewayServiceConfiguration.getPort();

        DiscoverableClientConfiguration discoverableClientConfiguration = ConfigReader.environmentConfiguration().getDiscoverableClientConfiguration();
        dcScheme = discoverableClientConfiguration.getScheme();
        dcHost = discoverableClientConfiguration.getHost();
        dcPort = discoverableClientConfiguration.getPort();

        RestAssured.port = gatewayPort;
        RestAssured.useRelaxedHTTPSValidation();

        requestUrl = String.format("%s://%s:%d%s%s%s", gatewayScheme, gatewayHost, gatewayPort, apiPrefix, "/" + serviceId, "/redirect");
    }

    /**
     * Test api instance of staticclient
     */
    @Test
    public void apiRouteOfDiscoverableClient() {
        String apiRelativeUrl = "/api/v1";
        String location = String.format("%s://%s:%d%s%s%s", dcScheme, dcHost, dcPort, baseUrl, apiRelativeUrl, "/greeting");
        String trasformedLocation = String.format("%s://%s:%d%s%s%s", gatewayScheme, gatewayHost, gatewayPort, apiPrefix, "/" + serviceId, "/greeting");

        RedirectLocation redirectLocation = new RedirectLocation(location);

        given()
            .contentType(JSON)
            .body(redirectLocation)
            .when()
            .post(requestUrl)
            .then()
            .statusCode(is(HttpStatus.TEMPORARY_REDIRECT.value()))
            .header(LOCATION, trasformedLocation);
    }

    /**
     * Test ws instance of staticclient
     */
    @Test
    public void wsRouteOfDiscoverableClient() {
        String wsRelativeUrl = "/ws";
        String location = String.format("%s://%s:%d%s%s", dcScheme, dcHost, dcPort, baseUrl, wsRelativeUrl);
        String wsPrefix = "/ws/v1";
        String trasformedLocation = String.format("%s://%s:%d%s%s", gatewayScheme, gatewayHost, gatewayPort, wsPrefix, "/" + serviceId);

        RedirectLocation redirectLocation = new RedirectLocation(location);

        given()
            .contentType(JSON)
            .body(redirectLocation)
            .when()
            .post(requestUrl)
            .then()
            .statusCode(is(HttpStatus.TEMPORARY_REDIRECT.value()))
            .header(LOCATION, trasformedLocation);
    }

    /**
     * Test ui instance of staticclient
     */
    @Test
    public void uiRouteOfDiscoverableClient() {
        String location = String.format("%s://%s:%d%s", dcScheme, dcHost, dcPort, baseUrl);
        String uiPrefix = "/ui/v1";
        String trasformedLocation = String.format("%s://%s:%d%s%s", gatewayScheme, gatewayHost, gatewayPort, uiPrefix, "/" + serviceId);

        RedirectLocation redirectLocation = new RedirectLocation(location);

        given()
            .contentType(JSON)
            .body(redirectLocation)
            .when()
            .post(requestUrl)
            .then()
            .statusCode(is(HttpStatus.TEMPORARY_REDIRECT.value()))
            .header(LOCATION, trasformedLocation);
    }

    class RedirectLocation {

        private String location;

        RedirectLocation(String location) {
            this.location = location;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }
    }
}
