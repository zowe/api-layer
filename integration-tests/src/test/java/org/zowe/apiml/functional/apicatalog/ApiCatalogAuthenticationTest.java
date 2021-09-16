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
import io.restassured.config.SSLConfig;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.zowe.apiml.util.categories.GeneralAuthenticationTest;
import org.zowe.apiml.util.config.*;
import org.zowe.apiml.util.service.DiscoveryUtils;

import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.util.http.HttpRequestUtils.getUriFromGateway;

@GeneralAuthenticationTest
class ApiCatalogAuthenticationTest {
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();

    private static final String CATALOG_PREFIX = "/api/v1";
    private static final String CATALOG_SERVICE_ID = "apicatalog";
    private static final String CATALOG_SERVICE_ID_PATH = "/" + CATALOG_SERVICE_ID;

    private static final String CATALOG_APIDOC_ENDPOINT = "/apidoc/discoverableclient/v1";
    private static final String CATALOG_STATIC_REFRESH_ENDPOINT = "/static-api/refresh";
    private static final String CATALOG_ACTUATOR_ENDPOINT = "/application";

    private final static String COOKIE = "apimlAuthenticationToken";
    private final static String BASIC_AUTHENTICATION_PREFIX = "Basic";
    private final static String INVALID_USERNAME = "incorrectUser";
    private final static String INVALID_PASSWORD = "incorrectPassword";

    private static String apiCatalogServiceUrl = ConfigReader.environmentConfiguration().getApiCatalogServiceConfiguration().getUrl();

    static Stream<Arguments> urlsToTest() {
        return Stream.of(
            Arguments.of(CATALOG_APIDOC_ENDPOINT),
            Arguments.of(CATALOG_STATIC_REFRESH_ENDPOINT),
            Arguments.of(CATALOG_ACTUATOR_ENDPOINT)
        );
    }

    static Stream<Arguments> urlsToTestWithCertificate() {
        return Stream.of(
            Arguments.of(CATALOG_APIDOC_ENDPOINT),
            Arguments.of(CATALOG_STATIC_REFRESH_ENDPOINT)
        );
    }

    @BeforeAll
    static void setUp() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());

        List<DiscoveryUtils.InstanceInfo> apiCatalogInstances = DiscoveryUtils.getInstances(CATALOG_SERVICE_ID);
        if (StringUtils.isEmpty(apiCatalogServiceUrl)) {
            apiCatalogServiceUrl = apiCatalogInstances.stream().findFirst().map(i -> String.format("%s", i.getUrl()))
                .orElseThrow(() -> new RuntimeException("Cannot determine API Catalog service from Discovery"));
        }
    }

    @BeforeEach
    void clearSsl() {
        RestAssured.config = RestAssured.config().sslConfig(SSLConfig.sslConfig());
        RestAssured.useRelaxedHTTPSValidation();
    }

    //@formatter:off
    @Nested
    class WhenAccessingCatalog {
        @Nested
        class ReturnOk {
            @ParameterizedTest(name = "givenValidBasicAuthentication {index} {0} ")
            @MethodSource("org.zowe.apiml.functional.apicatalog.ApiCatalogAuthenticationTest#urlsToTest")
            void givenValidBasicAuthentication(String endpoint) {
                given()
                    .auth().preemptive().basic(USERNAME, PASSWORD) // Isn't this kind of strange behavior?
                    .when()
                    .get(getUriFromGateway(CATALOG_PREFIX + CATALOG_SERVICE_ID_PATH + endpoint))
                    .then()
                    .statusCode(is(SC_OK));
            }

            @ParameterizedTest(name = "givenValidBasicAuthenticationAndCertificate {index} {0} ")
            @MethodSource("org.zowe.apiml.functional.apicatalog.ApiCatalogAuthenticationTest#urlsToTest")
            void givenValidBasicAuthenticationAndCertificate(String endpoint) {
                given()
                    .config(SslContext.clientCertApiml)
                    .auth().preemptive().basic(USERNAME, PASSWORD) // Isn't this kind of strange behavior?
                    .when()
                    .get(getUriFromGateway(CATALOG_PREFIX + CATALOG_SERVICE_ID_PATH + endpoint))
                    .then()
                    .statusCode(is(SC_OK));
            }
        }

        @Nested
        class ReturnUnauthorized {
            @ParameterizedTest(name = "givenNoAuthentication {index} {0}")
            @MethodSource("org.zowe.apiml.functional.apicatalog.ApiCatalogAuthenticationTest#urlsToTest")
            void givenNoAuthentication(String endpoint) {
                String expectedMessage = "Authentication is required for URL '" + CATALOG_SERVICE_ID_PATH + endpoint + "'";

                given()
                    .config(SslContext.tlsWithoutCert)
                    .when()
                    .get(getUriFromGateway(CATALOG_PREFIX + CATALOG_SERVICE_ID_PATH + endpoint))
                    .then()
                    .statusCode(is(SC_UNAUTHORIZED))
                    .header(HttpHeaders.WWW_AUTHENTICATE, BASIC_AUTHENTICATION_PREFIX)
                    .body(
                        "messages.find { it.messageNumber == 'ZWEAS105E' }.messageContent", equalTo(expectedMessage)
                    );
            }

            @ParameterizedTest(name = "givenInvalidBasicAuthentication {index} {0}")
            @MethodSource("org.zowe.apiml.functional.apicatalog.ApiCatalogAuthenticationTest#urlsToTest")
            void givenInvalidBasicAuthentication(String endpoint) {
                String expectedMessage = "Invalid username or password for URL '" + CATALOG_SERVICE_ID_PATH + endpoint + "'";

                given()
                    .auth().preemptive().basic(INVALID_USERNAME, INVALID_PASSWORD)
                    .when()
                    .get(getUriFromGateway(CATALOG_PREFIX + CATALOG_SERVICE_ID_PATH + endpoint))
                    .then()
                    .body(
                        "messages.find { it.messageNumber == 'ZWEAS120E' }.messageContent", equalTo(expectedMessage)
                    ).statusCode(is(SC_UNAUTHORIZED));
            }

            @ParameterizedTest(name = "givenInvalidTokenInCookie {index} {0}")
            @MethodSource("org.zowe.apiml.functional.apicatalog.ApiCatalogAuthenticationTest#urlsToTest")
            void givenInvalidTokenInCookie(String endpoint) {
                String expectedMessage = "Token is not valid for URL '" + CATALOG_SERVICE_ID_PATH + endpoint + "'";
                String invalidToken = "nonsense";

                given()
                    .cookie(COOKIE, invalidToken)
                    .when()
                    .get(getUriFromGateway(CATALOG_PREFIX + CATALOG_SERVICE_ID_PATH + endpoint))
                    .then()
                    .statusCode(is(SC_UNAUTHORIZED))
                    .body(
                        "messages.find { it.messageNumber == 'ZWEAS130E' }.messageContent", equalTo(expectedMessage)
                    );
            }
        }
    }

    @Nested
    class WhenAccessingWithCertificateViaServiceUrl {

        @Nested
        class WhenAccessApiDocRoute {

            @Nested
            class ThenReturnOk {
                @ParameterizedTest(name = "givenValidCertificate {index} {0} ")
                @MethodSource("org.zowe.apiml.functional.apicatalog.ApiCatalogAuthenticationTest#urlsToTestWithCertificate")
                void givenValidCertificate(String endpoint) {
                    given()
                        .config(SslContext.clientCertUser)
                        .when()
                        .get(apiCatalogServiceUrl + CATALOG_SERVICE_ID_PATH + endpoint)
                        .then()
                        .statusCode(HttpStatus.OK.value());
                }

                @ParameterizedTest(name = "givenValidCertificateAndBasicAuth {index} {0} ")
                @MethodSource("org.zowe.apiml.functional.apicatalog.ApiCatalogAuthenticationTest#urlsToTestWithCertificate")
                void givenValidCertificateAndBasicAuth(String endpoint) {
                    given()
                        .config(SslContext.clientCertApiml)
                        .auth().preemptive().basic(USERNAME, PASSWORD) // Isn't this kind of strange behavior?
                        .when()
                        .get(apiCatalogServiceUrl + CATALOG_SERVICE_ID_PATH + endpoint)
                        .then()
                        .statusCode(is(SC_OK));
                }
            }

            @Nested
            class ThenReturnUnauthorized {
                @ParameterizedTest(name = "givenUnTrustedCertificateAndNoBasicAuth_thenReturnUnauthorized {index} {0} ")
                @MethodSource("org.zowe.apiml.functional.apicatalog.ApiCatalogAuthenticationTest#urlsToTestWithCertificate")
                void givenUnTrustedCertificateAndNoBasicAuth_thenReturnUnauthorized(String endpoint) {
                    given()
                        .config(SslContext.selfSignedUntrusted)
                        .when()
                        .get(apiCatalogServiceUrl + CATALOG_SERVICE_ID_PATH + endpoint)
                        .then()
                        .statusCode(HttpStatus.UNAUTHORIZED.value());
                }

                @ParameterizedTest(name = "givenNoCertificateAndNoBasicAuth_thenReturnUnauthorized {index} {0} ")
                @MethodSource("org.zowe.apiml.functional.apicatalog.ApiCatalogAuthenticationTest#urlsToTestWithCertificate")
                void givenNoCertificateAndNoBasicAuth_thenReturnUnauthorized(String endpoint) {
                    given()
                        .when()
                        .get(apiCatalogServiceUrl + CATALOG_SERVICE_ID_PATH + endpoint)
                        .then()
                        .statusCode(HttpStatus.UNAUTHORIZED.value());
                }
            }
        }

        @Test
        void givenOnlyValidCertificate_whenAccessNotCertificateAuthedRoute_thenReturnUnauthorized() {
            given()
                .config(SslContext.clientCertApiml)
                .when()
                .get(apiCatalogServiceUrl + CATALOG_SERVICE_ID_PATH + CATALOG_ACTUATOR_ENDPOINT)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
        }
    }
}
