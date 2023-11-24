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
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.config.CloudGatewayConfiguration;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.ItSslConfigFactory;
import org.zowe.apiml.util.config.SslContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.*;
import static org.zowe.apiml.util.SecurityUtils.*;
import static org.zowe.apiml.util.requests.Endpoints.*;

@DiscoverableClientDependentTest
@Tag("CloudGatewayServiceRouting")
class CloudGatewayRoutingTest implements TestWithStartedInstances {

    private static final String HEADER_X_FORWARD_TO = "X-Forward-To";
    private static final String NON_EXISTING_SERVICE_ID = "noservice";
    private static final String NON_EXISTING_SERVICE_ENDPOINT = "/noservice/api/v1/something";
    private static final String WRONG_VERSION_ENPOINT = "/discoverableclient/api/v10/greeting";

    static CloudGatewayConfiguration conf = ConfigReader.environmentConfiguration().getCloudGatewayConfiguration();

    @BeforeAll
    static void setup() throws Exception {
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());

        RestAssured.useRelaxedHTTPSValidation();
    }

    @ParameterizedTest(name = "When base path is {0} should return 200")
    @CsvSource({
        "/apiml1" + DISCOVERABLE_GREET,
        DISCOVERABLE_GREET,
    })
    void testRoutingWithBasePath(String basePath) throws URISyntaxException {
        String scgUrl = String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), basePath);
        given().get(new URI(scgUrl)).then().statusCode(200);
    }

    @ParameterizedTest(name = "When header X-Forward-To is set to {0} should return 200")
    @CsvSource({
        "apiml1",
        "discoverableclient",
    })
    void testRoutingWithHeader(String forwardTo) throws URISyntaxException {
        String scgUrl = String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), DISCOVERABLE_GREET);
        given().header(HEADER_X_FORWARD_TO, forwardTo)
            .get(new URI(scgUrl)).then().statusCode(200);
    }

    @ParameterizedTest(name = "When base path is {0} should return 404")
    @CsvSource({
        "/apiml1" + NON_EXISTING_SERVICE_ENDPOINT,
        NON_EXISTING_SERVICE_ENDPOINT,
    })
    void testRoutingWithIncorrectServiceInBasePath(String basePath) throws URISyntaxException {
        String scgUrl = String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), basePath);
        given().get(new URI(scgUrl)).then().statusCode(404);
    }

    @ParameterizedTest(name = "When header X-Forward-To is set to {0} should return 404")
    @CsvSource({
        "apiml1",
        NON_EXISTING_SERVICE_ID,
    })
    void testRoutingWithIncorrectServiceInHeader(String forwardTo) throws URISyntaxException {
        String scgUrl = String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), NON_EXISTING_SERVICE_ENDPOINT);
        given().header(HEADER_X_FORWARD_TO, forwardTo)
            .get(new URI(scgUrl)).then().statusCode(404);
    }

    @ParameterizedTest(name = "When header X-Forward-To is set to {0} and base path is {1} should return 404")
    @CsvSource({
        "apiml1,/apiml1" + DISCOVERABLE_GREET,
    })
    void testWrongRoutingWithHeader(String forwardTo, String endpoint) throws URISyntaxException {
        String scgUrl = String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), endpoint);
        given().header(HEADER_X_FORWARD_TO, forwardTo)
            .get(new URI(scgUrl)).then().statusCode(404);
    }

    @ParameterizedTest(name = "When base path is {0} should return 404")
    @CsvSource({
        "/apiml1" + WRONG_VERSION_ENPOINT,
        WRONG_VERSION_ENPOINT,
    })
    void testWrongRoutingWithBasePath(String basePath) throws URISyntaxException {
        String scgUrl = String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), basePath);
        given().get(new URI(scgUrl)).then().statusCode(404);
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> validToBeTransformed() {
        return Stream.of(
            Arguments.of("Zowe auth scheme", ZOWE_JWT_REQUEST, (Consumer<Response>) response -> {
                assertNotNull(response.jsonPath().getString("cookies.apimlAuthenticationToken"));
                assertNull(response.jsonPath().getString("headers.authorization"));
                assertTrue(CollectionUtils.isEmpty(response.jsonPath().getList("certs")));
            }),
            Arguments.of("z/OSMF auth scheme", ZOSMF_REQUEST, (Consumer<Response>) response -> {
                assertNotNull(response.jsonPath().getString("cookies.jwtToken"));
                assertNull(response.jsonPath().getString("headers.authorization"));
                assertTrue(CollectionUtils.isEmpty(response.jsonPath().getList("certs")));
            }),
            Arguments.of("PassTicket auth scheme", REQUEST_INFO_ENDPOINT, (Consumer<Response>) response -> {
                assertNotNull(response.jsonPath().getString("headers.authorization"));
                assertTrue(response.jsonPath().getString("headers.authorization").startsWith("Basic "));
                assertTrue(CollectionUtils.isEmpty(response.jsonPath().getList("certs")));
            })
        );
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> noAuthTransformation() {
        Consumer<Response> assertions = response -> {
            assertEquals(200, response.getStatusCode());
            assertNull(response.jsonPath().getString("cookies.apimlAuthenticationToken"));
            assertNull(response.jsonPath().getString("cookies.jwtToken"));
            assertNull(response.jsonPath().getString("headers.authorization"));
            assertTrue(CollectionUtils.isEmpty(response.jsonPath().getList("certs")));
        };

        return Stream.of(
            Arguments.of("Zowe auth scheme", ZOWE_JWT_REQUEST, assertions),
            Arguments.of("z/OSMF auth scheme", ZOSMF_REQUEST, assertions),
            Arguments.of("PassTicket auth scheme", REQUEST_INFO_ENDPOINT, assertions)
        );
    }

    @Nested
    class AuthSchema {

        @Nested
        class ValidAuthScheme {

            @ParameterizedTest(name = "givenValidRequest_thenCredentialsAreTransformed {0} [{index}]")
            @MethodSource("org.zowe.apiml.functional.gateway.CloudGatewayRoutingTest#validToBeTransformed")
            void givenValidRequest_thenCredentialsAreTransformed(String title, String basePath, Consumer<Response> assertions) {
                String gatewayToken = SecurityUtils.gatewayToken(
                    ConfigReader.environmentConfiguration().getCredentials().getUser(),
                    ConfigReader.environmentConfiguration().getCredentials().getPassword()
                );

                Response response = given()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + gatewayToken)
                .when()
                    .get(String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), basePath));
                assertions.accept(response);
                assertEquals(200, response.getStatusCode());
            }

            @ParameterizedTest(name = "givenValidRequest_thenPatIsTransformed {0} [{index}]")
            @MethodSource("org.zowe.apiml.functional.gateway.CloudGatewayRoutingTest#validToBeTransformed")
            void givenValidRequest_thenPatIsTransformed(String title, String basePath, Consumer<Response> assertions) {
                String serviceId = basePath.substring(1, basePath.indexOf('/', 1));
                String pat = personalAccessToken(Collections.singleton(serviceId));

                Response response = given()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + pat)
                .when()
                    .get(String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), basePath));
                assertions.accept(response);
                assertEquals(200, response.getStatusCode());
            }

            @ParameterizedTest(name = "givenValidRequest_thenPatIsTransformed {0} [{index}]")
            @MethodSource("org.zowe.apiml.functional.gateway.CloudGatewayRoutingTest#validToBeTransformed")
            void givenValidRequest_thenClientCertIsTransformed(String title, String basePath, Consumer<Response> assertions) throws Exception {
                Response response = given()
                        .config(SslContext.clientCertValid)
                    .when()
                        .get(String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), basePath));
                assertions.accept(response);
                assertEquals(200, response.getStatusCode());
            }

            @ParameterizedTest(name = "givenValidRequest_thenOidcIsTransformed {0} [{index}]")
            @MethodSource("org.zowe.apiml.functional.gateway.CloudGatewayRoutingTest#validToBeTransformed")
            void givenValidRequest_thenOidcIsTransformed(String title, String basePath, Consumer<Response> assertions) {
                String oAuthToken = validOktaAccessToken(true);

                Response response = given()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + oAuthToken)
                .when()
                    .get(String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), basePath));
                assertions.accept(response);
                assertEquals(200, response.getStatusCode());
            }

        }

        @Nested
        class InvalidAuthScheme {

            @ParameterizedTest(name = "givenInvalidPatRequest_thenPatIsNotTransformed {0} [{index}]")
            @MethodSource("org.zowe.apiml.functional.gateway.CloudGatewayRoutingTest#noAuthTransformation")
            void givenInvalidPatRequest_thenPatIsNotTransformed(String title, String basePath, Consumer<Response> assertions) {
                String pat = personalAccessToken(Collections.singleton("anotherService"));

                Response response = given()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + pat)
                .when()
                    .get(String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), basePath));
                assertions.accept(response);
                assertEquals(200, response.getStatusCode());
            }

            @ParameterizedTest(name = "givenInvalidRequest_thenClientCertIsNotTransformed {0} [{index}]")
            @MethodSource("org.zowe.apiml.functional.gateway.CloudGatewayRoutingTest#noAuthTransformation")
            void givenInvalidRequest_thenClientCertIsNotTransformed(String title, String basePath, Consumer<Response> assertions) throws Exception {
                Response response = given()
                    .config(SslContext.selfSignedUntrusted)
                .when()
                    .get(String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), basePath));
                assertions.accept(response);
                assertEquals(200, response.getStatusCode());
            }

            @ParameterizedTest(name = "givenInvalidRequest_thenOidcIsNotTransformed {0} [{index}]")
            @MethodSource("org.zowe.apiml.functional.gateway.CloudGatewayRoutingTest#noAuthTransformation")
            void givenInvalidRequest_thenOidcIsNotTransformed(String title, String basePath, Consumer<Response> assertions) {
                String oAuthToken = generateJwtWithRandomSignature("https://localhost:10010");

                Response response = given()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + oAuthToken)
                    .when()
                    .get(String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), basePath));
                assertions.accept(response);
                assertEquals(200, response.getStatusCode());
            }

            @ParameterizedTest(name = "givenNoCredentials_thenNoCredentialsAreProvided {0} [{index}]")
            @MethodSource("org.zowe.apiml.functional.gateway.CloudGatewayRoutingTest#noAuthTransformation")
            void givenNoCredentials_thenNoCredentialsAreProvided(String title, String basePath, Consumer<Response> assertions) {
                Response response = when()
                    .get(String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), basePath));
                assertions.accept(response);
                assertEquals(200, response.getStatusCode());
            }

            @ParameterizedTest(name = "givenInvalidCredentials_thenNoCredentialsAreProvided {0} [{index}]")
            @MethodSource("org.zowe.apiml.functional.gateway.CloudGatewayRoutingTest#noAuthTransformation")
            void givenInvalidCredentials_thenNoCredentialsAreProvided(String title, String basePath, Consumer<Response> assertions) {
                Response response = given()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer invalidToken")
                .when()
                    .get(String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), basePath));
                assertions.accept(response);
                assertEquals(200, response.getStatusCode());
            }

        }

    }

}
