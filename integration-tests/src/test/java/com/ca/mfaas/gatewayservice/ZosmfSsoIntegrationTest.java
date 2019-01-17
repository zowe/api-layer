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
import com.ca.mfaas.utils.config.GatewayServiceConfiguration;
import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.core.Is.is;

public class ZosmfSsoIntegrationTest {
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getApiCatalogServiceConfiguration().getPassword();
    private final static String USERNAME = ConfigReader.environmentConfiguration().getApiCatalogServiceConfiguration().getUser();
    private final static String BASE_PATH = "/api/zosmf";
    private final static String ZOSMF_ENDPOINT = "/zosmf/restfiles/ds?dslevel=sys1.p*";

    private String token;
    private GatewayServiceConfiguration serviceConfiguration;
    private String scheme;
    private String host;
    private int port;

    @Before
    public void setUp() {
        serviceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        scheme = serviceConfiguration.getScheme();
        host = serviceConfiguration.getHost();
        port = serviceConfiguration.getPort();

        RestAssured.port = port;
        RestAssured.basePath = BASE_PATH;
        RestAssured.useRelaxedHTTPSValidation();
        token = SecurityUtils.gatewayToken(USERNAME, PASSWORD);
    }

    @Test
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
    public void doZosmfCallWithInvalidToken() {
        String invalidToken = "token";
        String expectedMessage = "Authentication problem: 'Token is not valid' for URL '/apicatalog/auth/query'";

        given()
            .header("Authorization", "Bearer " + invalidToken)
            .header("X-CSRF-ZOSMF-HEADER", "zosmf")
            .log().body()
        .when()
            .get(String.format("%s://%s:%d%s%s", scheme, host, port, BASE_PATH, ZOSMF_ENDPOINT))
        .then()
            .statusCode(is(SC_INTERNAL_SERVER_ERROR))
            .body(
                "messages.find { it.messageNumber == 'SEC0003' }.messageContent", equalTo(expectedMessage));
    }

    @Test
    public void doZosmfCallWithInvalidCookie() {
        String invalidToken = "token";
        String expectedMessage = "Authentication problem: 'Token is not valid' for URL '/apicatalog/auth/query'";

        given()
            .cookie("apimlAuthenticationToken", token)
            .header("X-CSRF-ZOSMF-HEADER", "zosmf")
            .log().body()
            .when()
            .get(String.format("%s://%s:%d%s%s", scheme, host, port, BASE_PATH, ZOSMF_ENDPOINT))
            .then()
            .statusCode(is(SC_INTERNAL_SERVER_ERROR))
            .body(
                "messages.find { it.messageNumber == 'SEC0003' }.messageContent", equalTo(expectedMessage));
    }

    @Test
    public void doZosmfCallWithoutToken() {
        int rc = 16;

        given()
            .header("X-CSRF-ZOSMF-HEADER", "zosmf")
        .when()
            .get(String.format("%s://%s:%d%s%s", scheme, host, port, BASE_PATH, ZOSMF_ENDPOINT))
        .then()
            .statusCode(is(SC_INTERNAL_SERVER_ERROR))
            .body(
                "rc", equalTo(rc));
    }

    @Test
    public void doZosmfCallWithEmptyHeader() {
        String emptyToken = " ";
        String expectedMessage = "Authentication problem: 'Valid token not provided.' for URL '/apicatalog/auth/query'";

        given()
            .header("Authorization", "Bearer " + emptyToken)
            .header("X-CSRF-ZOSMF-HEADER", "zosmf")
            .log().body()
        .when()
            .get(String.format("%s://%s:%d%s%s", scheme, host, port, BASE_PATH, ZOSMF_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'SEC0003' }.messageContent", equalTo(expectedMessage)
            );
    }


    @Test
    public void doZosmfCallWithEmptyCookie() {
        String emptyToken = " ";
        String expectedMessage = "Authentication problem: 'Valid token not provided.' for URL '/apicatalog/auth/query'";

        given()
            .cookie("apimlAuthenticationToken", token)
            .header("X-CSRF-ZOSMF-HEADER", "zosmf")
            .log().body()
        .when()
            .get(String.format("%s://%s:%d%s%s", scheme, host, port, BASE_PATH, ZOSMF_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'SEC0003' }.messageContent", equalTo(expectedMessage)
            );
    }
}
