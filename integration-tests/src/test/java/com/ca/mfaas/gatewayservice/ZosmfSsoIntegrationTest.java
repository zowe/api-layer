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

import com.ca.mfaas.util.categories.MainframeDependentTests;
import com.ca.mfaas.util.config.ConfigReader;
import com.ca.mfaas.util.config.GatewayServiceConfiguration;
import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.core.Is.is;

@Category(MainframeDependentTests.class)
public class ZosmfSsoIntegrationTest {
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private final static String BASE_PATH = "/api/zosmfca32";
    private final static String ZOSMF_ENDPOINT = "/zosmf/restfiles/ds?dslevel=sys1.p*";

    private String token;
    private String scheme;
    private String host;
    private int port;

    @Before
    public void setUp() {
        GatewayServiceConfiguration serviceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        scheme = serviceConfiguration.getScheme();
        host = serviceConfiguration.getHost();
        port = serviceConfiguration.getPort();

        RestAssured.port = port;
        RestAssured.basePath = BASE_PATH;
        RestAssured.useRelaxedHTTPSValidation();
        token = SecurityUtils.gatewayToken(USERNAME, PASSWORD);
    }

    @Test
    //@formatter:off
    public void doZosmfCallWithValidToken() {
        String dsname1 = "SYS1.PARMLIB";
        String dsname2 = "SYS1.PROCLIB";

        given()
            .header("Authorization", "Bearer " + token)
            .header("X-CSRF-ZOSMF-HEADER", "zosmf")
        .when()
            .get(String.format("%s://%s:%d%s%s", scheme, host, port, BASE_PATH, ZOSMF_ENDPOINT))
        .then()
            .statusCode(is(SC_OK))
            .body(
                "items.dsname", hasItems(dsname1, dsname2));
    }

    @Test
    public void doZosmfCallWithValidCookie() {
        String dsname1 = "SYS1.PARMLIB";
        String dsname2 = "SYS1.PROCLIB";

        given()
            .cookie("apimlAuthenticationToken", token)
            .header("X-CSRF-ZOSMF-HEADER", "zosmf")
        .when()
            .get(String.format("%s://%s:%d%s%s", scheme, host, port, BASE_PATH, ZOSMF_ENDPOINT))
        .then()
            .statusCode(is(SC_OK))
            .body(
                "items.dsname", hasItems(dsname1, dsname2));
    }

    @Test
    public void doZosmfCallWithValidLtpaCookie() {
        String dsname1 = "SYS1.PARMLIB";
        String dsname2 = "SYS1.PROCLIB";

        String ltpa = SecurityUtils.zosmfToken(USERNAME, PASSWORD);

        given()
            .cookie(SecurityUtils.ZOSMF_TOKEN, ltpa)
            .header("X-CSRF-ZOSMF-HEADER", "zosmf")
        .when()
            .get(String.format("%s://%s:%d%s%s", scheme, host, port, BASE_PATH, ZOSMF_ENDPOINT))
        .then()
            .statusCode(is(SC_OK))
            .body(
                "items.dsname", hasItems(dsname1, dsname2));
    }

    @Test
    public void doZosmfCallWithValidBasicHeader() {
        String dsname1 = "SYS1.PARMLIB";
        String dsname2 = "SYS1.PROCLIB";

        given()
            .auth().preemptive().basic(USERNAME, PASSWORD)
            .header("X-CSRF-ZOSMF-HEADER", "zosmf")
        .when()
            .get(String.format("%s://%s:%d%s%s", scheme, host, port, BASE_PATH, ZOSMF_ENDPOINT))
        .then()
            .statusCode(is(SC_OK))
            .body(
                "items.dsname", hasItems(dsname1, dsname2));
    }

    @Test
    public void doZosmfCallWithInvalidToken() {
        String invalidToken = "token";
        String expectedMessage = "Token is not valid";

        given()
            .header("Authorization", "Bearer " + invalidToken)
            .header("X-CSRF-ZOSMF-HEADER", "zosmf")
        .when()
            .get(String.format("%s://%s:%d%s%s", scheme, host, port, BASE_PATH, ZOSMF_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG102E' }.messageContent", equalTo(expectedMessage));
    }

    @Test
    public void doZosmfCallWithInvalidCookie() {
        String invalidToken = "token";
        String expectedMessage = "Token is not valid";

        given()
            .cookie("apimlAuthenticationToken", invalidToken)
            .header("X-CSRF-ZOSMF-HEADER", "zosmf")
        .when()
            .get(String.format("%s://%s:%d%s%s", scheme, host, port, BASE_PATH, ZOSMF_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG102E' }.messageContent", containsString(expectedMessage));
    }

    @Test
    public void doZosmfCallWithoutToken() {
        given()
            .header("X-CSRF-ZOSMF-HEADER", "zosmf")
        .when()
            .get(String.format("%s://%s:%d%s%s", scheme, host, port, BASE_PATH, ZOSMF_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED));
    }

    @Test
    public void doZosmfCallWithEmptyHeader() {
        String emptyToken = " ";

        given()
            .header("Authorization", "Bearer " + emptyToken)
            .header("X-CSRF-ZOSMF-HEADER", "zosmf")
        .when()
            .get(String.format("%s://%s:%d%s%s", scheme, host, port, BASE_PATH, ZOSMF_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED));
    }


    @Test
    public void doZosmfCallWithEmptyCookie() {
        String emptyToken = "";

        given()
            .cookie("apimlAuthenticationToken", emptyToken)
            .header("X-CSRF-ZOSMF-HEADER", "zosmf")
        .when()
            .get(String.format("%s://%s:%d%s%s", scheme, host, port, BASE_PATH, ZOSMF_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED));
    }
    //@formatter:on
}
