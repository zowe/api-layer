/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.functional.gateway;

import io.restassured.RestAssured;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.GatewayTest;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;

import static io.restassured.RestAssured.given;

/**
 * This is an integration test class for ValidateAPIController.java
 * It tests ValidateAPIController with service verificationOnboardService.java
 * <p>
 * The test scenario is:
 * Send serviceId to validateAPIController then the controller will call verificationOnboardService.java
 * to check if the service is registered and metadata can be retrieved then it will return appropriate message
 * The controller receive the response from the service and then will post response.
 */
@GatewayTest
class ValidateAPITest implements TestWithStartedInstances {
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    String token = SecurityUtils.gatewayToken(USERNAME, PASSWORD);

    @BeforeEach
    public void relaxedHTTPS() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    @TestsNotMeantForZowe
    void testPostEndpoint() {
        given()
            .log().all()
            .param("serviceID", "discoverableclient")
            .header("Cookie", "apimlAuthenticationToken=" + token)
            .when()
            .post(getLegacyEndpointURLPost())
            .then()
            .assertThat()
            .statusCode(HttpStatus.SC_BAD_REQUEST);

    }

    @Test
    @TestsNotMeantForZowe
    void testGetEndpoint() {
        given()
            .log().all()
            .header("Cookie", "apimlAuthenticationToken=" + token)
            .when()
            .get(getEndpointURLGet() + "/discoverableclient")
            .then()
            .assertThat()
            .statusCode(HttpStatus.SC_BAD_REQUEST);

    }

    @Test
    @TestsNotMeantForZowe
    void testGetEndpointNonConformant() {
        given()
            .log().all()
            .header("Cookie", "apimlAuthenticationToken=" + token)
            .when()
            .get(getEndpointURLGet() + "/nonConformantServiceBecauseNameIsTooLongAndContainsCapitalLettersqwertyuiop")
            .then()
            .assertThat()
            .statusCode(HttpStatus.SC_BAD_REQUEST);

    }

    @Test
    @TestsNotMeantForZowe
    void testGetEndpointWithNoAuthentication() {
        given()
            .log().all()
        .when()
            .get(getEndpointURLGet() + "/discoverableclient")
        .then()
            .assertThat()
            .statusCode(HttpStatus.SC_UNAUTHORIZED);

    }

    @Test
    @TestsNotMeantForZowe
    void testLegacyEndpointWithNoAuthentication() {
        given()
            .log().all()
            .param("serviceID", "discoverableclient")
        .when()
            .post(getLegacyEndpointURLPost())
        .then()
            .assertThat()
            .statusCode(HttpStatus.SC_UNAUTHORIZED);

    }

    private String getEndpointURLGet() {
        GatewayServiceConfiguration gatewayServiceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        String gatewayScheme = gatewayServiceConfiguration.getScheme();
        String gatewayHost = gatewayServiceConfiguration.getHost();
        int gatewayPort = gatewayServiceConfiguration.getExternalPort();
        RestAssured.port = gatewayPort;
        RestAssured.useRelaxedHTTPSValidation();

        return String.format("%s://%s:%d%s", gatewayScheme, gatewayHost, gatewayPort, "/gateway/api/v1/conformance");
    }

    private String getLegacyEndpointURLPost() {
        GatewayServiceConfiguration gatewayServiceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        String gatewayScheme = gatewayServiceConfiguration.getScheme();
        String gatewayHost = gatewayServiceConfiguration.getHost();
        int gatewayPort = gatewayServiceConfiguration.getExternalPort();
        RestAssured.port = gatewayPort;
        RestAssured.useRelaxedHTTPSValidation();

        return String.format("%s://%s:%d%s", gatewayScheme, gatewayHost, gatewayPort, "/validate");
    }
}
