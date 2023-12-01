/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.authentication.schemes;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.ZaasTest;
import org.zowe.apiml.util.config.CloudGatewayConfiguration;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.ItSslConfigFactory;
import org.zowe.apiml.util.config.SslContext;

import java.util.Collections;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.*;
import static org.zowe.apiml.util.SecurityUtils.*;
import static org.zowe.apiml.util.requests.Endpoints.*;
import static org.zowe.apiml.util.requests.Endpoints.REQUEST_INFO_ENDPOINT;

@ZaasTest
public class CloudGatewayAuthTest implements TestWithStartedInstances {

    private static final CloudGatewayConfiguration conf = ConfigReader.environmentConfiguration().getCloudGatewayConfiguration();

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
            Arguments.of("SAF IDT auth scheme", SAF_IDT_REQUEST, (Consumer<Response>) response -> {
                assertNull(response.jsonPath().getString("cookies.jwtToken"));
                assertNotNull(response.jsonPath().getString("headers.x-saf-token"));
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

    @BeforeAll
    static void setup() throws Exception {
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());

        RestAssured.useRelaxedHTTPSValidation();
    }

    @Nested
    class ValidAuthScheme {

        @ParameterizedTest(name = "givenValidRequest_thenCredentialsAreTransformed {0} [{index}]")
        @MethodSource("org.zowe.apiml.integration.authentication.schemes.CloudGatewayAuthTest#validToBeTransformed")
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
        @MethodSource("org.zowe.apiml.integration.authentication.schemes.CloudGatewayAuthTest#validToBeTransformed")
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
        @MethodSource("org.zowe.apiml.integration.authentication.schemes.CloudGatewayAuthTest#validToBeTransformed")
        void givenValidRequest_thenClientCertIsTransformed(String title, String basePath, Consumer<Response> assertions) {
            Response response = given()
                .config(SslContext.clientCertValid)
            .when()
                .get(String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), basePath));
            assertions.accept(response);
            assertEquals(200, response.getStatusCode());
        }

        @ParameterizedTest(name = "givenValidRequest_thenOidcIsTransformed {0} [{index}]")
        @MethodSource("org.zowe.apiml.integration.authentication.schemes.CloudGatewayAuthTest#validToBeTransformed")
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
        @MethodSource("org.zowe.apiml.integration.authentication.schemes.CloudGatewayAuthTest#noAuthTransformation")
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
        @MethodSource("org.zowe.apiml.integration.authentication.schemes.CloudGatewayAuthTest#noAuthTransformation")
        void givenInvalidRequest_thenClientCertIsNotTransformed(String title, String basePath, Consumer<Response> assertions) {
            Response response = given()
                .config(SslContext.selfSignedUntrusted)
            .when()
                .get(String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), basePath));
            assertions.accept(response);
            assertEquals(200, response.getStatusCode());
        }

        @ParameterizedTest(name = "givenInvalidRequest_thenOidcIsNotTransformed {0} [{index}]")
        @MethodSource("org.zowe.apiml.integration.authentication.schemes.CloudGatewayAuthTest#noAuthTransformation")
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
        @MethodSource("org.zowe.apiml.integration.authentication.schemes.CloudGatewayAuthTest#noAuthTransformation")
        void givenNoCredentials_thenNoCredentialsAreProvided(String title, String basePath, Consumer<Response> assertions) {
            Response response = when()
                .get(String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), basePath));
            assertions.accept(response);
            assertEquals(200, response.getStatusCode());
        }

        @ParameterizedTest(name = "givenInvalidCredentials_thenNoCredentialsAreProvided {0} [{index}]")
        @MethodSource("org.zowe.apiml.integration.authentication.schemes.CloudGatewayAuthTest#noAuthTransformation")
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
