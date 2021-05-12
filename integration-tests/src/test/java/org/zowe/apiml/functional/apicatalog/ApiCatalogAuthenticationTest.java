/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.functional.apicatalog;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.zowe.apiml.util.categories.GeneralAuthenticationTest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.http.HttpRequestUtils;
import org.zowe.apiml.util.service.DiscoveryUtils;

import java.net.URI;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;

/**
 * Have a combination of valid and invalid methods to verify. The integration should span the major components and verify
 * that
 *
 * Discovery
 * API Catalog
 * Caching service
 */
@GeneralAuthenticationTest
public class ApiCatalogAuthenticationTest {
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();

    private static final String CATALOG_PREFIX = "/api/v1";
    private static final String CATALOG_SERVICE_ID = "/apicatalog";

    private static final String CATALOG_APIDOC_ENDPOINT = "/apidoc/discoverableclient/v1";
    private static final String CATALOG_ACTUATOR_ENDPOINT = "/application";

    private final static String COOKIE = "apimlAuthenticationToken";
    private final static String BASIC_AUTHENTICATION_PREFIX = "Basic";
    private final static String INVALID_USERNAME = "incorrectUser";
    private final static String INVALID_PASSWORD = "incorrectPassword";

    static Stream<Arguments> urlsToTest() {
        return Stream.of(
            Arguments.of(CATALOG_APIDOC_ENDPOINT),
            Arguments.of(CATALOG_ACTUATOR_ENDPOINT)
        );
    }

    URI getUrl(String endpoint) {
        return HttpRequestUtils.getUriFromGateway(CATALOG_PREFIX + CATALOG_SERVICE_ID + endpoint);
    }

    @BeforeEach
    void setUp() {
        RestAssured.useRelaxedHTTPSValidation(); // Why do we use the relaxed validation.
    }

    //@formatter:off
    @Nested
    class GivenValidAuthentication {
        @ParameterizedTest
        @MethodSource("org.zowe.apiml.functional.apicatalog.ApiCatalogAuthenticationTest#urlsToTest")
        void viaBasic_returnOk(String endpoint) {
            given()
                .auth().preemptive().basic(USERNAME, PASSWORD) // Isn't this kind of strange behavior?
            .when()
                .get(getUrl(endpoint))
            .then()
                .statusCode(is(SC_OK));
        }
    }

    @Nested
    class GivenInvalidAuthentication {
        @ParameterizedTest
        @MethodSource("org.zowe.apiml.functional.apicatalog.ApiCatalogAuthenticationTest#urlsToTest")
        void whenSendNothing_returnUnauthorized(String endpoint) {
            String expectedMessage = "Authentication is required for URL '" + CATALOG_SERVICE_ID + endpoint + "'";

            given()
            .when()
                .get(getUrl(endpoint))
            .then()
                .statusCode(is(SC_UNAUTHORIZED))
                .header(HttpHeaders.WWW_AUTHENTICATE, BASIC_AUTHENTICATION_PREFIX)
                .body("messages.find { it.messageNumber == 'ZWEAS105E' }.messageContent", equalTo(expectedMessage)
                );
        }

        @ParameterizedTest
        @MethodSource("org.zowe.apiml.functional.apicatalog.ApiCatalogAuthenticationTest#urlsToTest")
        void whenSendInvalidViaBasic_returnUnauthorized(String endpoint) {
            String expectedMessage = "Invalid username or password for URL '" + CATALOG_SERVICE_ID + endpoint + "'";

            given()
                .auth().preemptive().basic(INVALID_USERNAME, INVALID_PASSWORD)
            .when()
                .get(getUrl(endpoint))
            .then()
                .statusCode(is(SC_UNAUTHORIZED))
                .body("messages.find { it.messageNumber == 'ZWEAS120E' }.messageContent", equalTo(expectedMessage));
        }

        @ParameterizedTest
        @MethodSource("org.zowe.apiml.functional.apicatalog.ApiCatalogAuthenticationTest#urlsToTest")
        void whenSendInvalidToken_returnUnauthorized(String endpoint) {
            String expectedMessage = "Token is not valid for URL '" + CATALOG_SERVICE_ID + endpoint + "'";
            String invalidToken = "nonsense";

            given()
                .cookie(COOKIE, invalidToken)
            .when()
                .get(getUrl(endpoint))
            .then()
                .statusCode(is(SC_UNAUTHORIZED))
                .body("messages.find { it.messageNumber == 'ZWEAS130E' }.messageContent", equalTo(expectedMessage));
        }
    }
}
