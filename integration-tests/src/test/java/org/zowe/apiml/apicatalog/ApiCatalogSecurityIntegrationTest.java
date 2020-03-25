/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog;

import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.http.HttpHeaders;
import org.zowe.apiml.gatewayservice.SecurityUtils;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.service.DiscoveryUtils;

import java.util.Arrays;
import java.util.Collection;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.gatewayservice.SecurityUtils.getConfiguredSslConfig;

@RunWith(value = Parameterized.class)
public class ApiCatalogSecurityIntegrationTest {
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private final static String GATEWAY_SCHEME = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getScheme();
    private final static String GATEWAY_HOST = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getHost();
    private final static int GATEWAY_PORT = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getPort();

    private static final String CATALOG_PREFIX = "/api/v1";
    private static final String CATALOG_SERVICE_ID = "/apicatalog";
    private static final String CATALOG_APIDOC_ENDPOINT = "/apidoc/discoverableclient/v1";
    private static final String CATALOG_ACTUATOR_ENDPOINT = "/application";

    private final static String COOKIE = "apimlAuthenticationToken";
    private final static String BASIC_AUTHENTICATION_PREFIX = "Basic";
    private final static String INVALID_USERNAME = "incorrectUser";
    private final static String INVALID_PASSWORD = "incorrectPassword";

    private final String endpoint;

    public ApiCatalogSecurityIntegrationTest(String endpoint) {
        this.endpoint = endpoint;
    }

    @Parameterized.Parameters(name = "{index}: Endpoint '{0}'")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {CATALOG_APIDOC_ENDPOINT},
            {CATALOG_ACTUATOR_ENDPOINT}
        });
    }

    @Before
    public void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    //@formatter:off
    @Test
    public void accessProtectedEndpointWithoutCredentials() {
        String expectedMessage = "Authentication is required for URL '" + CATALOG_SERVICE_ID + endpoint + "'";

        given()
        .when()
            .get(String.format("%s://%s:%d%s%s%s", GATEWAY_SCHEME, GATEWAY_HOST, GATEWAY_PORT, CATALOG_PREFIX,
                CATALOG_SERVICE_ID, endpoint))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .header(HttpHeaders.WWW_AUTHENTICATE, BASIC_AUTHENTICATION_PREFIX)
            .body("messages.find { it.messageNumber == 'ZWEAS105E' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Test
    public void accessProtectedEndpointWithBasicAuth() {
        given()
            .auth().preemptive().basic(USERNAME, PASSWORD)
        .when()
            .get(String.format("%s://%s:%d%s%s%s", GATEWAY_SCHEME, GATEWAY_HOST, GATEWAY_PORT, CATALOG_PREFIX,
                CATALOG_SERVICE_ID, endpoint))
        .then()
            .statusCode(is(SC_OK));
    }

    @Test
    public void loginToGatewayAndAccessProtectedEndpointWithCookie() {
        String token = SecurityUtils.gatewayToken(USERNAME, PASSWORD);

        given()
            .cookie(COOKIE, token)
        .when()
            .get(String.format("%s://%s:%d%s%s%s", GATEWAY_SCHEME, GATEWAY_HOST, GATEWAY_PORT, CATALOG_PREFIX,
                CATALOG_SERVICE_ID, endpoint))
        .then()
            .statusCode(is(SC_OK));
    }

    @Test
    public void accessProtectedEndpointWithInvalidBasicAuth() {
        String expectedMessage = "Invalid username or password for URL '" + CATALOG_SERVICE_ID + endpoint + "'";

        given()
            .auth().preemptive().basic(INVALID_USERNAME, INVALID_PASSWORD)
        .when()
            .get(String.format("%s://%s:%d%s%s%s", GATEWAY_SCHEME, GATEWAY_HOST, GATEWAY_PORT, CATALOG_PREFIX,
                CATALOG_SERVICE_ID, endpoint))
        .then()
            .statusCode(is(SC_UNAUTHORIZED));
    }

    @Test
    public void accessProtectedEndpointWithInvalidCookieCatalog() {
        String expectedMessage = "Token is not valid for URL '" + CATALOG_SERVICE_ID + endpoint + "'";
        String invalidToken = "nonsense";

        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        String catalogUrl = DiscoveryUtils.getInstances("APICATALOG").get(0).getUrl();

        given()
            .cookie(COOKIE, invalidToken)
        .when()
            .get(String.format("%s%s%s", catalogUrl, CATALOG_SERVICE_ID, endpoint))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body("messages.find { it.messageNumber == 'ZWEAS130E' }.messageContent", equalTo(expectedMessage));
    }

    @Test
    public void accessProtectedEndpointWithInvalidCookieGateway() {
        String invalidToken = "nonsense";

        given()
             .cookie(COOKIE, invalidToken)
        .when()
            .get(String.format("%s://%s:%d%s%s%s", GATEWAY_SCHEME, GATEWAY_HOST, GATEWAY_PORT, CATALOG_PREFIX,
                CATALOG_SERVICE_ID, endpoint))
        .then()
            .statusCode(is(SC_UNAUTHORIZED));
    }

    //@formatter:on
}
