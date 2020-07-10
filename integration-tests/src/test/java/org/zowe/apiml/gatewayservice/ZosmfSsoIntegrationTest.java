/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gatewayservice;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.categories.MainframeDependentTests;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.core.Is.is;

@MainframeDependentTests
public class ZosmfSsoIntegrationTest {
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private final static String ZOSMF_SERVICE_ID = ConfigReader.environmentConfiguration().getZosmfServiceConfiguration().getServiceId();
    private final static String BASE_PATH = "/api/" + ZOSMF_SERVICE_ID;
    private final static String ZOSMF_ENDPOINT = "/zosmf/restfiles/ds?dslevel=sys1.p*";

    private String token;
    private String scheme;
    private String host;
    private int port;

    @BeforeEach
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
    void doZosmfCallWithValidCookie() {
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
    void doZosmfCallWithValidLtpaCookie() {
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
    void doZosmfCallWithValidBasicHeader() {
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
    void doZosmfCallWithInvalidToken() {
        String invalidToken = "token";
        String expectedMessage = "Token is not valid";

        given()
            .header("Authorization", "Bearer " + invalidToken)
            .header("X-CSRF-ZOSMF-HEADER", "zosmf")
        .when()
            .get(String.format("%s://%s:%d%s%s", scheme, host, port, BASE_PATH, ZOSMF_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED));
    }

    @Test
    void doZosmfCallWithInvalidCookie() {
        String invalidToken = "token";
        String expectedMessage = "Token is not valid";

        given()
            .cookie("apimlAuthenticationToken", invalidToken)
            .header("X-CSRF-ZOSMF-HEADER", "zosmf")
        .when()
            .get(String.format("%s://%s:%d%s%s", scheme, host, port, BASE_PATH, ZOSMF_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED));
    }

    @Test
    void doZosmfCallWithoutToken() {
        given()
            .header("X-CSRF-ZOSMF-HEADER", "zosmf")
        .when()
            .get(String.format("%s://%s:%d%s%s", scheme, host, port, BASE_PATH, ZOSMF_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED));
    }

    @Test
    void doZosmfCallWithEmptyHeader() {
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
    void doZosmfCallWithEmptyCookie() {
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
