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
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.config.CloudGatewayConfiguration;
import org.zowe.apiml.util.config.ConfigReader;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.zowe.apiml.util.requests.Endpoints.*;

@DiscoverableClientDependentTest
@Tag("CloudGatewayServiceRouting")
class CloudGatewayRoutingTest implements TestWithStartedInstances {

    private static final String HEADER_X_FORWARD_TO = "X-Forward-To";
    private static final String NON_EXISTING_SERVICE_ID = "noservice";
    private static final String NON_EXISTING_SERVICE_ENDPOINT = "/noservice/api/v1/something";
    private static final String WRONG_VERSION_ENPOINT = "/discoverableclient/api/v10/greeting";

    static CloudGatewayConfiguration conf = ConfigReader.environmentConfiguration().getCloudGatewayConfiguration();

    @BeforeEach
    void setup() {
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
            Arguments.of("z/OSMF auth scheme", ZOSMF_REQUEST, (Consumer<Response>) response -> {
                assertEquals(200, response.getStatusCode());
                assertNotNull(response.jsonPath().getString("cookies.jwtToken"));
                assertNull(response.jsonPath().getString("headers.authorization"));
                //TODO: uncomment once http client handle client certs propertly
                //assertTrue(CollectionUtils.isEmpty(response.jsonPath().getList("certs")));
            }),
            Arguments.of("passticket auth scheme", REQUEST_INFO_ENDPOINT, (Consumer<Response>) response -> {
                assertEquals(200, response.getStatusCode());
                assertNotNull(response.jsonPath().getString("headers.authorization"));
                assertTrue(response.jsonPath().getString("headers.authorization").startsWith("Basic "));
                //TODO: uncomment once http client handle client certs propertly
                //assertTrue(CollectionUtils.isEmpty(response.jsonPath().getList("certs")));
            })
        );
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> noCredentials() {
        Consumer<Response> assertions = response -> {
            assertEquals(200, response.getStatusCode());
            assertNull(response.jsonPath().getString("cookies.jwtToken"));
            assertNull(response.jsonPath().getString("headers.authorization"));
            //TODO: uncomment once http client handle client certs propertly
            //assertTrue(CollectionUtils.isEmpty(response.jsonPath().getList("certs")));
        };

        return Stream.of(
            Arguments.of("z/OSMF auth scheme", ZOSMF_REQUEST, assertions),
            Arguments.of("passticket auth scheme", REQUEST_INFO_ENDPOINT, assertions)
        );
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class AuthSchema {

        private String token;

        @BeforeAll
        void setCredentials() {
            token = SecurityUtils.gatewayToken(
                ConfigReader.environmentConfiguration().getCredentials().getUser(),
                ConfigReader.environmentConfiguration().getCredentials().getPassword()
            );
        }

        @ParameterizedTest(name = "givenValidRequest_thenCredentialsAreTransformed {0} [{index}]")
        @MethodSource("org.zowe.apiml.functional.gateway.CloudGatewayRoutingTest#validToBeTransformed")
        <T> void givenValidRequest_thenCredentialsAreTransformed(String title, String basePath, Consumer<Response> assertions) {
            Response response = given()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .when()
                    .get(String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), basePath));
            assertions.accept(response);
        }

        @ParameterizedTest(name = "givenNoCredentials_thenNoCredentialsAreProvided {0} [{index}]")
        @MethodSource("org.zowe.apiml.functional.gateway.CloudGatewayRoutingTest#noCredentials")
        <T> void givenNoCredentials_thenNoCredentialsAreProvided(String title, String basePath, Consumer<Response> assertions) {
            Response response = given().when()
                .get(String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), basePath));
            assertions.accept(response);
        }

        @ParameterizedTest(name = "givenInvalidCredentials_thenNoCredentialsAreProvided {0} [{index}]")
        @MethodSource("org.zowe.apiml.functional.gateway.CloudGatewayRoutingTest#noCredentials")
        <T> void givenInvalidCredentials_thenNoCredentialsAreProvided(String title, String basePath, Consumer<Response> assertions) {
            Response response = given().header(HttpHeaders.AUTHORIZATION, "Bearer invalidToken")
                .when()
                    .get(String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), basePath));
            assertions.accept(response);
        }

    }

}
