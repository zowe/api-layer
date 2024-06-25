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
import org.apache.http.message.BasicNameValuePair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.GeneralAuthenticationTest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.ItSslConfigFactory;
import org.zowe.apiml.util.config.SslContext;

import java.net.URI;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.util.SecurityUtils.GATEWAY_TOKEN_COOKIE_NAME;
import static org.zowe.apiml.util.http.HttpRequestUtils.getUriFromGateway;
import static org.zowe.apiml.util.requests.Endpoints.ROUTED_SERVICE;
import static org.zowe.apiml.util.requests.Endpoints.ROUTED_SERVICE_NOT_VERSIONED;

/**
 * Verify it's possible to retrieve information about services onboarded to the gateway if the user requesting the
 * information has valid authorization in the ESM.
 * <p>
 * The integration is about verifying the integration with ESM.
 */
@GeneralAuthenticationTest
class ServicesInfoTest implements TestWithStartedInstances {

    public static final String VERSION_HEADER = "Content-Version";
    public static final String CURRENT_VERSION = "1";

    private final static String USERNAME = ConfigReader.environmentConfiguration().getAuxiliaryUserList().getCredentials("servicesinfo-authorized").get(0).getUser();
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getAuxiliaryUserList().getCredentials("servicesinfo-authorized").get(0).getPassword();

    private final static String UNAUTHORIZED_USERNAME = ConfigReader.environmentConfiguration().getAuxiliaryUserList().getCredentials("servicesinfo-unauthorized").get(0).getUser();
    private final static String UNAUTHORIZED_PASSWORD = ConfigReader.environmentConfiguration().getAuxiliaryUserList().getCredentials("servicesinfo-unauthorized").get(0).getPassword();

    private static final String API_CATALOG_SERVICE_ID = "apicatalog";
    private static final String API_CATALOG_SERVICE_API_ID = "zowe.apiml.apicatalog";
    private static final String API_CATALOG_PATH = "/apicatalog/api/v1";

    private static String token;

    @BeforeAll
    public static void init() throws Exception {
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());
    }

    @Nested
    class WhenGettingInformationAboutServices {

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        class ReturnUnauthorized {
            Stream<String> endpoints() {
                return Stream.of(
                    ROUTED_SERVICE,
                    ROUTED_SERVICE_NOT_VERSIONED,
                    ROUTED_SERVICE + "/" + API_CATALOG_SERVICE_ID,
                    ROUTED_SERVICE_NOT_VERSIONED + "/" + API_CATALOG_SERVICE_ID
                );
            }

            @ParameterizedTest(name = "givenNoAuthentication {index} {0} ")
            @MethodSource("endpoints")
            void givenNoAuthentication(String endpoint) {
                //@formatter:off
                given().config(SslContext.tlsWithoutCert)
                    .when()
                    .get(getUriFromGateway(endpoint))
                    .then()
                    .statusCode(is(SC_UNAUTHORIZED));
                //@formatter:on
            }
        }

        @Nested
        class GivenClientCertificateCallDirectlyTowardsGateway {
            @ParameterizedTest(name = "givenClientCertificate_returns200WithoutSafCheck {index} {0} ")
            @ValueSource(strings = {
                ROUTED_SERVICE_NOT_VERSIONED,
                ROUTED_SERVICE_NOT_VERSIONED + "/" + API_CATALOG_SERVICE_ID
            })
            void returns200WithoutSafCheck(String endpoint) {
                given().config(SslContext.clientCertValid)
                    .when()
                    .get(getUriFromGateway(endpoint))
                    .then()
                    .statusCode(is(SC_OK));
            }

            @ParameterizedTest(name = "givenClientCertificate_returns401WithUntrustedCert {index} {0} ")
            @ValueSource(strings = {
                ROUTED_SERVICE_NOT_VERSIONED,
                ROUTED_SERVICE_NOT_VERSIONED + "/" + API_CATALOG_SERVICE_ID
            })
            void returns401WithUntrustedCert(String endpoint) {
                given().config(SslContext.selfSignedUntrusted)
                    .when()
                    .get(getUriFromGateway(endpoint))
                    .then()
                    .statusCode(is(SC_UNAUTHORIZED));
            }
        }

        @Nested
        class ReturnAllServices {
            @BeforeEach
            void setUp() {
                RestAssured.useRelaxedHTTPSValidation();
                RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

                token = SecurityUtils.gatewayToken(USERNAME, PASSWORD);
            }

            @Test
            @SuppressWarnings({"squid:S2699", "Assets are after then()"})
            void givenValidToken() {
                //@formatter:off
                given()
                    .cookie(GATEWAY_TOKEN_COOKIE_NAME, token)
                    .when()
                    .get(getUriFromGateway(ROUTED_SERVICE))
                    .then()
                    .statusCode(is(SC_OK))
                    .header(VERSION_HEADER, CURRENT_VERSION)
                    .body("serviceId", hasItems("gateway", "discovery", API_CATALOG_SERVICE_ID));
                //@formatter:on
            }
        }

        @Nested
        class ReturnForbidden {
            @Test
            @SuppressWarnings({"squid:S2699", "Assets are after then()"})
            void givenInvalidCredentials() {
                String expectedMessage = "The user is not authorized to the target resource:";

                //@formatter:off
                given()
                    .auth().basic(UNAUTHORIZED_USERNAME, UNAUTHORIZED_PASSWORD)
                    .when()
                    .get(getUriFromGateway(ROUTED_SERVICE))
                    .then()
                    .statusCode(is(SC_FORBIDDEN))
                    .body("messages.find { it.messageNumber == 'ZWEAT403E' }.messageContent", startsWith(expectedMessage));
                //@formatter:on
            }
        }
    }

    protected static Stream<Arguments> serviceSpecificUrls() {
        return Stream.of(
            Arguments.of(getUriFromGateway(ROUTED_SERVICE + "/" + API_CATALOG_SERVICE_ID), API_CATALOG_SERVICE_ID, API_CATALOG_SERVICE_API_ID, API_CATALOG_PATH),
            Arguments.of(getUriFromGateway(ROUTED_SERVICE + "/gateway"), "gateway", "zowe.apiml.gateway", "/gateway/api/v1")
        );
    }

    @Nested
    class WhenGettingDetailsAboutSpecificService {
        @BeforeEach
        void setUp() {
            RestAssured.useRelaxedHTTPSValidation();
            RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

            token = SecurityUtils.gatewayToken(USERNAME, PASSWORD);
        }

        @Nested
        class ReturnDetailedInformationInObject {
            @ParameterizedTest(name = "givenValidTokenWithAuthorizedUserAndValidServiceId {index} {0} {1} {2} {3}")
            @MethodSource("org.zowe.apiml.integration.zos.ServicesInfoTest#serviceSpecificUrls")
            @SuppressWarnings({"squid:S2699", "Assets are after then()"})
            void givenValidTokenWithAuthorizedUserAndValidServiceId(URI uri, String serviceId, String serviceApiId, String servicePath) {
                //@formatter:off
                given()
                    .cookie(GATEWAY_TOKEN_COOKIE_NAME, token)
                    .when()
                    .get(uri)
                    .then()
                    .statusCode(is(SC_OK))
                    .header(VERSION_HEADER, CURRENT_VERSION)

                    .body("serviceId", is(serviceId))
                    .body("apiml.apiInfo[0].apiId", is(serviceApiId))
                    .body("apiml.apiInfo[0].basePath", is(servicePath));

                //@formatter:on
            }
        }

        @Nested
        class ReturnDetailedInformationInArray {
            @Test
            @SuppressWarnings({"squid:S2699", "Assets are after then()"})
            void givenValidTokenWithAuthorizedUserAndValidServiceId() {
                //@formatter:off
                given()
                    .cookie(GATEWAY_TOKEN_COOKIE_NAME, token)
                    .when()
                    .get(getUriFromGateway(ROUTED_SERVICE, new BasicNameValuePair("apiId", API_CATALOG_SERVICE_API_ID)))
                    .then()
                    .statusCode(is(SC_OK))
                    .header(VERSION_HEADER, CURRENT_VERSION)

                    .body("serviceId", hasItem(API_CATALOG_SERVICE_ID))
                    .body("apiml.apiInfo[0].apiId", hasItem(API_CATALOG_SERVICE_API_ID))
                    .body("apiml.apiInfo[0].basePath", hasItem(API_CATALOG_PATH));

                //@formatter:on
            }
        }
    }
}
