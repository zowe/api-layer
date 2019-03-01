/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.test.integration.gatewayservice;

import com.broadcom.apiml.test.integration.utils.config.ConfigReader;
import com.broadcom.apiml.test.integration.utils.config.GatewayServiceConfiguration;
import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;

public class QueryIntegrationTest {
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getApiCatalogServiceConfiguration().getPassword();
    private final static String USERNAME = ConfigReader.environmentConfiguration().getApiCatalogServiceConfiguration().getUser();
    private final static String QUERY_ENDPOINT = "/auth/query";

    private String token;
    private GatewayServiceConfiguration serviceConfiguration;
    private String scheme;
    private String host;
    private int port;
    private String basePath;

    @Before
    public void setUp() {
        serviceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        scheme = serviceConfiguration.getScheme();
        host = serviceConfiguration.getHost();
        port = serviceConfiguration.getPort();
        basePath = "/api/v1/gateway";

        RestAssured.port = port;
        RestAssured.basePath = basePath;
        RestAssured.useRelaxedHTTPSValidation();
        token = SecurityUtils.gatewayToken(USERNAME, PASSWORD);
    }

    @Test
    public void doQueryWithValidToken() {
        given()
            .header("Authorization", "Bearer " + token)
            .when()
            .get(String.format("%s://%s:%d%s%s", scheme, host, port, basePath, QUERY_ENDPOINT))
            .then()
            .statusCode(is(SC_OK))
            .body(
                "userId", equalTo(USERNAME)
            );
    }

    @Test
    public void doQueryWithInvalidToken() {
        String invalidToken = "1234";
        String expectedMessage = "Authentication problem: 'Token is not valid' for URL '/apicatalog/auth/query'";

        given()
            .header("Authorization", "Bearer " + invalidToken)
            .contentType(JSON)
            .when()
            .get(String.format("%s://%s:%d%s%s", scheme, host, port, basePath, QUERY_ENDPOINT))
            .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'SEC0003' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Test
    public void doQueryWithoutHeader() {
        String expectedMessage = "Authentication problem: 'Valid token not provided.' for URL '/apicatalog/auth/query'";

        given()
            .when()
            .get(String.format("%s://%s:%d%s%s", scheme, host, port, basePath, QUERY_ENDPOINT))
            .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'SEC0003' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Test
    public void doQueryWithWrongAuthType() {
        String expectedMessage = "Authentication problem: 'Valid token not provided.' for URL '/apicatalog/auth/query'";

        given()
            .header("Authorization", "Basic " + token)
            .when()
            .get(String.format("%s://%s:%d%s%s", scheme, host, port, basePath, QUERY_ENDPOINT))
            .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'SEC0003' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Test
    public void doQueryWithEmptyHeader() {
        String emptyToken = " ";
        String expectedMessage = "Authentication problem: 'Token is not valid' for URL '/apicatalog/auth/query'";

        given()
            .header("Authorization", "Bearer " + emptyToken)
            .when()
            .get(String.format("%s://%s:%d%s%s", scheme, host, port, basePath, QUERY_ENDPOINT))
            .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'SEC0003' }.messageContent", equalTo(expectedMessage)
            );
    }
}
