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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.zowe.apiml.util.categories.NotForMainframeTest;
import org.zowe.apiml.util.config.ConfigReader;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.constants.ApimlConstants.BASIC_AUTHENTICATION_PREFIX;
import static org.zowe.apiml.gatewayservice.SecurityUtils.GATEWAY_TOKEN_COOKIE_NAME;

@NotForMainframeTest // Remove when secured with an authorization
class ServicesInfoTest {

    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private final static String SCHEME = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration()
            .getScheme();
    private final static String HOST = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration()
            .getHost();
    private final static int PORT = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getPort();

    private static final String SERVICES_ENDPOINT = "gateway/api/v1/services";
    private static final String SERVICES_ENDPOINT_NOT_VERSIONED = "gateway/services";
    private static final String API_CATALOG_SERVICE = "apicatalog";

    private String token;

    @BeforeEach
    void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        token = SecurityUtils.gatewayToken(USERNAME, PASSWORD);
    }

    @ParameterizedTest(name = "cannotBeAccessedWithoutAuthentication({0})")
    @ValueSource(strings = {
            SERVICES_ENDPOINT + "/" + API_CATALOG_SERVICE,
            SERVICES_ENDPOINT_NOT_VERSIONED + "/" + API_CATALOG_SERVICE
    })
    void cannotBeAccessedWithoutAuthentication(String endpoint) {
        //@formatter:off
        when()
            .get(String.format("%s://%s:%d/%s", SCHEME, HOST, PORT, endpoint))
       .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .header(HttpHeaders.WWW_AUTHENTICATE, BASIC_AUTHENTICATION_PREFIX);
        //@formatter:on
    }

    @Test
    @SuppressWarnings({"squid:S2699", "Assets are after then()"})
    void providesApiCatalogServiceInformation() {
        //@formatter:off
        given()
                .cookie(GATEWAY_TOKEN_COOKIE_NAME, token)
       .when()
                .get(String.format("%s://%s:%d/%s/%s", SCHEME, HOST, PORT, SERVICES_ENDPOINT, API_CATALOG_SERVICE))
       .then()
                .statusCode(is(SC_OK)).body("apiml.apiInfo[0].apiId", equalTo("zowe.apiml.apicatalog"))
                .body("apiml.apiInfo[0].basePath", equalTo("/apicatalog/api/v1"));
        //@formatter:on
    }

    @Test
    @SuppressWarnings({"squid:S2699", "Assets are after then()"})
    void providesGatewayServiceInformation() {
        //@formatter:off
        given()
                .cookie(GATEWAY_TOKEN_COOKIE_NAME, token)
       .when()
                .get(String.format("%s://%s:%d/%s/%s", SCHEME, HOST, PORT, SERVICES_ENDPOINT, "gateway"))
       .then()
                .statusCode(is(SC_OK)).body("apiml.apiInfo[0].apiId", equalTo("zowe.apiml.gateway"));
        //@formatter:on
    }

}
