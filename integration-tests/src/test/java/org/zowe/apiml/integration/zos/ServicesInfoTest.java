/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.integration.zos;

import io.restassured.RestAssured;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.GeneralAuthenticationTest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.constants.ApimlConstants.BASIC_AUTHENTICATION_PREFIX;
import static org.zowe.apiml.util.SecurityUtils.GATEWAY_TOKEN_COOKIE_NAME;

@GeneralAuthenticationTest
class ServicesInfoTest implements TestWithStartedInstances {

    public static final String VERSION_HEADER = "Content-Version";
    public static final String CURRENT_VERSION = "1";

    private final static String USERNAME = ConfigReader.environmentConfiguration().getAuxiliaryUserList().getCredentials("servicesinfo-authorized").get(0).getUser();
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getAuxiliaryUserList().getCredentials("servicesinfo-authorized").get(0).getPassword();

    private final static String UNAUTHORIZED_USERNAME = ConfigReader.environmentConfiguration().getAuxiliaryUserList().getCredentials("servicesinfo-unauthorized").get(0).getUser();
    private final static String UNAUTHORIZED_PASSWORD = ConfigReader.environmentConfiguration().getAuxiliaryUserList().getCredentials("servicesinfo-unauthorized").get(0).getPassword();

    private static final String SERVICES_ENDPOINT = "gateway/api/v1/services";
    private static final String SERVICES_ENDPOINT_NOT_VERSIONED = "gateway/services";
    private static final String API_CATALOG_SERVICE_ID = "apicatalog";
    private static final String API_CATALOG_SERVICE_API_ID = "zowe.apiml.apicatalog";

    private static String token;

    @BeforeEach
    void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        token = SecurityUtils.gatewayToken(USERNAME, PASSWORD);
    }

    @ParameterizedTest(name = "cannotBeAccessedWithoutAuthentication({0})")
    @ValueSource(strings = {
        SERVICES_ENDPOINT,
        SERVICES_ENDPOINT_NOT_VERSIONED,
        SERVICES_ENDPOINT + "/" + API_CATALOG_SERVICE_ID,
        SERVICES_ENDPOINT_NOT_VERSIONED + "/" + API_CATALOG_SERVICE_ID
    })
    void cannotBeAccessedWithoutAuthentication(String endpoint) {
        //@formatter:off
        when()
            .get(HttpRequestUtils.getUriFromGateway(endpoint))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .header(HttpHeaders.WWW_AUTHENTICATE, BASIC_AUTHENTICATION_PREFIX);
        //@formatter:on
    }

    @Test
    @SuppressWarnings({"squid:S2699", "Assets are after then()"})
    void providesAllServicesInformation() {
        //@formatter:off
        given()
            .cookie(GATEWAY_TOKEN_COOKIE_NAME, token)
        .when()
            .get(HttpRequestUtils.getUriFromGateway(SERVICES_ENDPOINT))
        .then()
            .statusCode(is(SC_OK))
            .header(VERSION_HEADER, CURRENT_VERSION)
            .body("serviceId", hasItems("gateway", "discovery", API_CATALOG_SERVICE_ID));
        //@formatter:on
    }

    @Test
    @SuppressWarnings({"squid:S2699", "Assets are after then()"})
    void providesServicesInformationByApiId() {
        List<NameValuePair> arguments = new ArrayList<>();
        arguments.add(new BasicNameValuePair("apiId", API_CATALOG_SERVICE_API_ID));

        //@formatter:off
        given()
            .cookie(GATEWAY_TOKEN_COOKIE_NAME, token)
        .when()
            .get(HttpRequestUtils.getUriFromGateway(SERVICES_ENDPOINT, arguments))
        .then()
            .statusCode(is(SC_OK))
            .header(VERSION_HEADER, CURRENT_VERSION)
            .body("size()", is(1))
            .body("serviceId", hasItem(API_CATALOG_SERVICE_ID));
        //@formatter:on
    }

    @Test
    @SuppressWarnings({"squid:S2699", "Assets are after then()"})
    void providesApiCatalogServiceInformation() {
        //@formatter:off
        given()
            .cookie(GATEWAY_TOKEN_COOKIE_NAME, token)
        .when()
            .get(HttpRequestUtils.getUriFromGateway(SERVICES_ENDPOINT + "/" + API_CATALOG_SERVICE_ID))
        .then()
            .statusCode(is(SC_OK))
            .header(VERSION_HEADER, CURRENT_VERSION)
            .body("apiml.apiInfo[0].apiId", equalTo(API_CATALOG_SERVICE_API_ID))
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
            .get(HttpRequestUtils.getUriFromGateway(SERVICES_ENDPOINT + "/gateway"))
        .then()
            .statusCode(is(SC_OK))
            .header(VERSION_HEADER, CURRENT_VERSION)
            .body("apiml.apiInfo[0].apiId", equalTo("zowe.apiml.gateway"));
        //@formatter:on
    }

    @Test
    @SuppressWarnings({"squid:S2699", "Assets are after then()"})
    void whenUnauthorized_thenReturn403() {
        String expectedMessage = "The user is not authorized to the target resource:";

        //@formatter:off
        given()
            .auth().basic(UNAUTHORIZED_USERNAME, UNAUTHORIZED_PASSWORD)
        .when()
            .get(HttpRequestUtils.getUriFromGateway(SERVICES_ENDPOINT))
        .then()
            .statusCode(is(SC_FORBIDDEN))
            .body("messages.find { it.messageNumber == 'ZWEAT403E' }.messageContent", startsWith(expectedMessage));
        //@formatter:on
    }

}
