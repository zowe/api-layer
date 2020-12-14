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

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.constants.ApimlConstants.BASIC_AUTHENTICATION_PREFIX;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.zowe.apiml.util.config.ConfigReader;

import io.restassured.RestAssured;

class ServicesInfoTest {
    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private final static String SCHEME = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration()
            .getScheme();
    private final static String HOST = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration()
            .getHost();
    private final static int PORT = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getPort();

    private static final String SERVICES_ENDPOINT = "gateway/api/v1/services";
    private static final String API_CATALOG_SERVICE = "apicatalog";

    private String token;

    @BeforeEach
    void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        token = SecurityUtils.gatewayToken(USERNAME, PASSWORD);
    }

    @Test
    void cannotBeAccessedWithoutAuthentication() {
        given().when()
                .get(String.format("%s://%s:%d/%s/%s", SCHEME, HOST, PORT, SERVICES_ENDPOINT, API_CATALOG_SERVICE))
                .then().statusCode(is(SC_UNAUTHORIZED))
                .header(HttpHeaders.WWW_AUTHENTICATE, BASIC_AUTHENTICATION_PREFIX);
    }

    @Test
    void providesApiCatalogServiceInformation() {
        given().header("Authorization", "Bearer " + token).when()
                .get(String.format("%s://%s:%d/%s/%s", SCHEME, HOST, PORT, SERVICES_ENDPOINT, API_CATALOG_SERVICE))
                .then().statusCode(is(SC_OK)).body("apiml.apiInfo[0].apiId", equalTo("zowe.apiml.apicatalog"))
                .body("apiml.apiInfo[0].basePath", equalTo("/apicatalog/api/v1"));
    }

    @Test
    void providesGatewayServiceInformation() {
        given().header("Authorization", "Bearer " + token).when()
                .get(String.format("%s://%s:%d/%s/%s", SCHEME, HOST, PORT, SERVICES_ENDPOINT, "gateway")).then()
                .statusCode(is(SC_OK)).body("apiml.apiInfo[0].apiId", equalTo("zowe.apiml.gateway"));
    }
}
