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

import com.ca.mfaas.constants.ApimlConstants;
import com.ca.mfaas.utils.config.ConfigReader;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GatewaySecurityTest {
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private final static String SCHEME = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getScheme();
    private final static String HOST = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getHost();
    private final static int PORT = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getPort();
    private static final String PROTECTED_ENDPOINT = "/application/routes";
    private final static String COOKIE = "apimlAuthenticationToken";

    @Before
    public void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    //@formatter:off
    @Test
    public void accessProtectedEndpointWithoutCredentials() {
        given()
        .when()
            .get(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, PROTECTED_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .header(HttpHeaders.WWW_AUTHENTICATE, ApimlConstants.BASIC_AUTHENTICATION_PREFIX);
    }

    @Test
    public void loginToGatewayAndAccessProtectedEndpointWithBasicAuthentication() {
        given()
            .auth().preemptive().basic(USERNAME, PASSWORD)
        .when()
            .get(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, PROTECTED_ENDPOINT))
        .then()
            .statusCode(is(SC_OK));
    }

    @Test
    public void loginToGatewayAndAccessProtectedEndpointWithCookie() {
        String token = SecurityUtils.gatewayToken(USERNAME, PASSWORD);

        given()
            .cookie(COOKIE, token)
        .when()
            .get(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, PROTECTED_ENDPOINT))
        .then()
            .statusCode(is(SC_OK));
    }

    @Test
    public void accessProtectedEndpointWithInvalidToken() {
        String invalidToken = "badToken";

        given()
            .cookie(COOKIE, invalidToken)
        .when()
            .get(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, PROTECTED_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED));
    }


    @Test
    public void accessProtectedEndpointWithInvalidCredentials() {
        String invalidPassword = "badPassword";

        given()
            .auth().preemptive().basic(USERNAME, invalidPassword)
        .when()
            .get(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, PROTECTED_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED));
    }

    @Test
    public void verifyHttpHeaders() {
        String token = SecurityUtils.gatewayToken(USERNAME, PASSWORD);

        Map<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put("X-Content-Type-Options","nosniff");
        expectedHeaders.put("X-XSS-Protection","1; mode=block");
        expectedHeaders.put("Cache-Control","no-cache, no-store, max-age=0, must-revalidate");
        expectedHeaders.put("Pragma","no-cache");
        expectedHeaders.put("Content-Type","text/html;charset=UTF-8");
        expectedHeaders.put("Transfer-Encoding","chunked");

        List<String> forbiddenHeaders = new ArrayList<>();
        forbiddenHeaders.add("X-Frame-Options");
        forbiddenHeaders.add("Strict-Transport-Security");

        Response response =  RestAssured.given().cookie(COOKIE, token)
                            .get(String.format("%s://%s:%d", SCHEME, HOST, PORT));
        Map<String,String> responseHeaders = new HashMap<>();
        response.getHeaders().forEach(h -> responseHeaders.put(h.getName(),h.getValue()));

        expectedHeaders.entrySet().forEach(h -> assertThat(responseHeaders, hasEntry(h.getKey(),h.getValue())));
        forbiddenHeaders.forEach(h -> assertThat(responseHeaders, not(hasKey(h))));
    }
}
