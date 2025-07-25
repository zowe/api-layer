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

import groovy.util.logging.Slf4j;
import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import io.restassured.response.Validatable;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.categories.GeneralAuthenticationTest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.ItSslConfigFactory;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.service.DiscoveryUtils;

import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.fail;
import static org.zowe.apiml.util.http.HttpRequestUtils.getUriFromGateway;

@GeneralAuthenticationTest
@Slf4j
class ApiCatalogAuthenticationTest {

    private static final boolean IS_MODULITH_ENABLED = Boolean.getBoolean("environment.modulith");

    private static final String UNAUTHENTICATED_ERROR_NUMBER = IS_MODULITH_ENABLED ? "ZWEAG120E" : "ZWEAS120E";

    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();

    private static final String CATALOG_SERVICE_ID = "apicatalog";
    private static final String CATALOG_SERVICE_ID_PATH = "/" + CATALOG_SERVICE_ID;
    private static final String CATALOG_PREFIX = "/api/v1";

    private static final String CATALOG_APIDOC_ENDPOINT = "/apidoc/discoverableclient/zowe.apiml.discoverableclient.rest v1.0.0";
    private static final String CATALOG_STATIC_REFRESH_ENDPOINT = "/static-api/refresh";
    private static final String CATALOG_APPLICATION_ENDPOINT = "/application";
    private static final String CATALOG_HEALTH_ENDPOINT = "/application/health";
    private final static String COOKIE = "apimlAuthenticationToken";
    private final static String BASIC_AUTHENTICATION_PREFIX = "Basic";
    private final static String INVALID_USERNAME = "incorrectUser";
    private final static String INVALID_PASSWORD = "incorrectPassword";

    private static String apiCatalogServiceUrl = ConfigReader.environmentConfiguration().getApiCatalogServiceConfiguration().getUrl();

    @FunctionalInterface
    private interface Request {
        Validatable<?, ?> execute(RequestSpecification when, String endpoint);
    }

    static Stream<Arguments> requestsToTest() {
        var arguments = new LinkedList<Arguments>();
        arguments.add(
            Arguments.of(CATALOG_APIDOC_ENDPOINT, (Request) (when, endpoint) ->
                when.urlEncodingEnabled(false) // space in URL gets encoded by getUriFromGateway
                    .get(getUriFromGateway(CATALOG_SERVICE_ID_PATH + CATALOG_PREFIX + endpoint))
            )
        );
        arguments.add(
            Arguments.of(CATALOG_STATIC_REFRESH_ENDPOINT, (Request) (when, endpoint) -> when.post(getUriFromGateway(CATALOG_SERVICE_ID_PATH + CATALOG_PREFIX + endpoint)))
        );

        if (!IS_MODULITH_ENABLED) {
            arguments.add(
                Arguments.of(CATALOG_APPLICATION_ENDPOINT, (Request) (when, endpoint) -> when.get(getUriFromGateway(CATALOG_SERVICE_ID_PATH + CATALOG_PREFIX + endpoint)))
            );
        }

        return arguments.stream();
    }

    static Stream<Arguments> requestsToTestWithCertificate() {
        return Stream.of(
            Arguments.of(CATALOG_SERVICE_ID_PATH + (IS_MODULITH_ENABLED ? CATALOG_PREFIX : "") + CATALOG_APIDOC_ENDPOINT, (Request) (when, endpoint) -> when.get(apiCatalogServiceUrl + endpoint)),
            Arguments.of(CATALOG_SERVICE_ID_PATH + (IS_MODULITH_ENABLED ? CATALOG_PREFIX : "") + CATALOG_STATIC_REFRESH_ENDPOINT, (Request) (when, endpoint) -> when.post(apiCatalogServiceUrl + endpoint))
        );
    }

    @BeforeAll
    static void setUp() throws Exception {
        var catalogConfig = ConfigReader.environmentConfiguration().getApiCatalogServiceConfiguration();
        RestAssured.useRelaxedHTTPSValidation();
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());

        List<DiscoveryUtils.InstanceInfo> apiCatalogInstances = DiscoveryUtils.getInstances(CATALOG_SERVICE_ID);
        if (StringUtils.isEmpty(apiCatalogServiceUrl)) {
            apiCatalogServiceUrl = apiCatalogInstances.stream()
                .filter(catalogInstance -> catalogInstance.getPort() == catalogConfig.getPort())
                .findFirst()
                .map(i -> String.format("%s", i.getUrl()))
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
            @MethodSource("org.zowe.apiml.functional.apicatalog.ApiCatalogAuthenticationTest#requestsToTest")
            void givenValidBasicAuthentication(String endpoint, Request request) {
                request.execute(
                        given()
                            .auth().basic(USERNAME, PASSWORD)
                            .when(),
                        endpoint
                    )
                    .then()
                    .log().all()
                    .statusCode(is(SC_OK));
            }

            @ParameterizedTest(name = "givenValidBearerAuthentication {index} {0} ")
            @MethodSource("org.zowe.apiml.functional.apicatalog.ApiCatalogAuthenticationTest#requestsToTest")
            void givenValidBearerAuthentication(String endpoint, Request request) {
                String token = SecurityUtils.gatewayToken(USERNAME, PASSWORD);
                request.execute(
                        given()
                            .header("Authorization", "Bearer " + token)
                            .when(),
                        endpoint
                    )
                    .then()
                    .statusCode(is(SC_OK));
            }

            @ParameterizedTest(name = "givenValidBasicAuthenticationAndCertificate {index} {0} ")
            @MethodSource("org.zowe.apiml.functional.apicatalog.ApiCatalogAuthenticationTest#requestsToTest")
            void givenValidBasicAuthenticationAndCertificate(String endpoint, Request request) {
                request.execute(
                        given()
                            .config(SslContext.clientCertApiml)
                            .auth().basic(USERNAME, PASSWORD)
                            .when(),
                        endpoint
                    )
                    .then()
                    .log().all()
                    .statusCode(is(SC_OK));
            }
        }

        @Nested
        class ReturnUnauthorized {
            @ParameterizedTest(name = "givenNoAuthentication {index} {0}")
            @MethodSource("org.zowe.apiml.functional.apicatalog.ApiCatalogAuthenticationTest#requestsToTest")
            void givenNoAuthentication(String endpoint, Request request) throws URISyntaxException {
                String expectedMessage = "Invalid username or password for URL '" + CATALOG_SERVICE_ID_PATH + (IS_MODULITH_ENABLED ? CATALOG_PREFIX : "") + new URIBuilder().setPath(endpoint).build() + "'";

                request.execute(
                        given()
                            .config(SslContext.tlsWithoutCert)
                            .when(),
                        endpoint
                    )
                    .then()
                    .log().ifValidationFails()
                    .statusCode(is(SC_UNAUTHORIZED))
                    .header(HttpHeaders.WWW_AUTHENTICATE, BASIC_AUTHENTICATION_PREFIX)
                    .body(
                        "messages.find { it.messageNumber == '" + UNAUTHENTICATED_ERROR_NUMBER + "' }.messageContent", equalTo(expectedMessage)
                    );
            }

            @ParameterizedTest(name = "givenInvalidBasicAuthentication {index} {0}")
            @MethodSource("org.zowe.apiml.functional.apicatalog.ApiCatalogAuthenticationTest#requestsToTest")
            void givenInvalidBasicAuthentication(String endpoint, Request request) throws URISyntaxException {
                String expectedMessage = "Invalid username or password for URL '" + CATALOG_SERVICE_ID_PATH + (IS_MODULITH_ENABLED ? CATALOG_PREFIX : "") + new URIBuilder().setPath(endpoint).build() + "'";

                request.execute(
                        given()
                            .log().all()
                            .auth().basic(INVALID_USERNAME, INVALID_PASSWORD)
                            .when(),
                        endpoint
                    )
                    .then()
                        .statusCode(is(SC_UNAUTHORIZED))
                        .body(
                            "messages.find { it.messageNumber == '" + UNAUTHENTICATED_ERROR_NUMBER + "' }.messageContent", equalTo(expectedMessage)
                        );
            }

            @ParameterizedTest(name = "givenInvalidBearerAuthentication {index} {0}")
            @MethodSource("org.zowe.apiml.functional.apicatalog.ApiCatalogAuthenticationTest#requestsToTest")
            void givenInvalidBearerAuthentication(String endpoint, Request request) {
                String expectedMessage = "The request has not been applied because it lacks valid authentication credentials.";

                request.execute(
                        given()
                            .header("Authorization", "Bearer invalidToken")
                            .when(),
                        endpoint
                    )
                    .then()
                    .log().ifValidationFails()
                    .body(
                        "messages.find { it.messageNumber == 'ZWEAO402E' }.messageContent", equalTo(expectedMessage)
                    ).statusCode(is(SC_UNAUTHORIZED));
            }

            @ParameterizedTest(name = "givenInvalidTokenInCookie {index} {0}")
            @MethodSource("org.zowe.apiml.functional.apicatalog.ApiCatalogAuthenticationTest#requestsToTest")
            void givenInvalidTokenInCookie(String endpoint, Request request)  {
                String expectedMessage = "The request has not been applied because it lacks valid authentication credentials.";
                String invalidToken = "nonsense";

                request.execute(
                        given()
                            .cookie(COOKIE, invalidToken)
                            .when(),
                        endpoint
                    )
                    .then()
                    .statusCode(is(SC_UNAUTHORIZED))
                    .body(
                        "messages.find { it.messageNumber == 'ZWEAO402E' }.messageContent", equalTo(expectedMessage)
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
                @MethodSource("org.zowe.apiml.functional.apicatalog.ApiCatalogAuthenticationTest#requestsToTestWithCertificate")
                void givenValidCertificate(String endpoint, Request request) {
                    request.execute(
                            given()
                                .config(SslContext.clientCertUser)
                                .when(),
                            endpoint
                        )
                        .then()
                        .log().all()
                        .statusCode(HttpStatus.OK.value());
                }

                @ParameterizedTest(name = "givenValidCertificateAndBasicAuth {index} {0} ")
                @MethodSource("org.zowe.apiml.functional.apicatalog.ApiCatalogAuthenticationTest#requestsToTestWithCertificate")
                void givenValidCertificateAndBasicAuth(String endpoint, Request request) {
                    request.execute(
                            given()
                                .config(SslContext.clientCertApiml)
                                .auth().basic(USERNAME, PASSWORD)
                                .when(),
                            endpoint
                        )
                        .then()
                        .statusCode(is(SC_OK));
                }
            }

            @Nested
            class ThenReturnUnauthorized {
                @ParameterizedTest(name = "givenUnTrustedCertificateAndNoBasicAuth_thenReturnUnauthorized {index} {0} ")
                @MethodSource("org.zowe.apiml.functional.apicatalog.ApiCatalogAuthenticationTest#requestsToTestWithCertificate")
                void givenUnTrustedCertificateAndNoBasicAuth_thenReturnUnauthorized(String endpoint, Request request) {
                    request.execute(
                            given()
                                .config(SslContext.selfSignedUntrusted)
                                .when(),
                            endpoint
                        )
                        .then()
                        .statusCode(HttpStatus.UNAUTHORIZED.value());
                }

                @ParameterizedTest(name = "givenNoCertificateAndNoBasicAuth_thenReturnUnauthorized {index} {0} ")
                @MethodSource("org.zowe.apiml.functional.apicatalog.ApiCatalogAuthenticationTest#requestsToTestWithCertificate")
                void givenNoCertificateAndNoBasicAuth_thenReturnUnauthorized(String endpoint, Request request) {
                    request.execute(
                            given()
                                .when(),
                            endpoint
                        )
                        .then()
                        .statusCode(HttpStatus.UNAUTHORIZED.value());
                }
            }
        }

        @Test
        @DisabledIfSystemProperty(
            disabledReason = "In Modulith, API Catalog does not have its own /application/** endpoints",
            named = "environment.modulith",
            matches = "true"
        )
        void givenOnlyValidCertificate_whenAccessNotCertificateAuthedRoute_thenReturnUnauthorized() {
            try {
                given()
                    .config(SslContext.clientCertApiml)
                .when()
                    .get(apiCatalogServiceUrl + CATALOG_SERVICE_ID_PATH + CATALOG_APPLICATION_ENDPOINT)
                .then()
                    .statusCode(HttpStatus.UNAUTHORIZED.value());
            } catch (Exception e) {
                fail("Failure to GET " + apiCatalogServiceUrl + CATALOG_SERVICE_ID_PATH + CATALOG_APPLICATION_ENDPOINT);
            }

        }

    }

    @Test
    @DisplayName("This test needs to run against catalog service instance that has application/health endpoint authentication enabled.")
    @DisabledIfSystemProperty(
        disabledReason = "In Modulith, API Catalog does not have its own /application/** endpoints",
        named = "environment.modulith",
        matches = "true"
    )
    void thenDoNotAuthenticate() {
        try {
            given()
            .when()
                .get(apiCatalogServiceUrl + CATALOG_SERVICE_ID_PATH + CATALOG_HEALTH_ENDPOINT)
            .then()
                .statusCode(is(SC_UNAUTHORIZED));
        } catch (Exception e) {
            fail("Failure to GET " + apiCatalogServiceUrl + CATALOG_SERVICE_ID_PATH + CATALOG_HEALTH_ENDPOINT, e);
        }
    }

    @Test
    @DisplayName("This test needs to run against catalog service instance that has application/health endpoint authentication provided.")
    @DisabledIfSystemProperty(
        disabledReason = "In Modulith, API Catalog does not have its own /application/** endpoints",
        named = "environment.modulith",
        matches = "true"
    )
    void thenAuthenticateTheRequest() {
        try {
            given()
                .auth().basic(USERNAME, PASSWORD)
            .when()
                .get(apiCatalogServiceUrl + CATALOG_SERVICE_ID_PATH + CATALOG_HEALTH_ENDPOINT)
            .then()
                .statusCode(is(SC_OK));
        } catch (Exception e) {
            fail("Failure to GET " + apiCatalogServiceUrl + CATALOG_SERVICE_ID_PATH + CATALOG_HEALTH_ENDPOINT, e);
        }

    }

}
