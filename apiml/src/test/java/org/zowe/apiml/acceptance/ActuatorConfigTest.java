/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.PlainJWT;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.zowe.apiml.handler.LocalTokenProvider;
import org.zowe.apiml.security.common.token.QueryResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;

import static io.restassured.RestAssured.basePath;
import static io.restassured.RestAssured.given;
import static org.apache.hc.core5.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.hc.core5.http.HttpStatus.SC_METHOD_NOT_ALLOWED;
import static org.apache.hc.core5.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.hc.core5.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;
import static org.apache.hc.core5.http.HttpStatus.SC_UNAUTHORIZED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ActuatorConfigTest {

    private static final String USER = "USER";

    private static final String USER_NO_PERMISSION = "APIMTST";

    private static final String AUTH_COOKIE = "apimlAuthenticationToken";

    private abstract static class ActuatorAcceptanceTest extends AcceptanceTestWithBasePath {

        @MockitoBean
        private LocalTokenProvider tokenProvider;

        private RestAssuredConfig config;

        @BeforeEach
        void mockTokenValidation() {
            when(tokenProvider.validateToken(any())).thenAnswer(invocation -> {
                String jwt = invocation.getArgument(0);
                String userId = JWTParser.parse(jwt).getJWTClaimsSet().getSubject();
                return Mono.just(new QueryResponse(
                    null, userId, new Date(), Date.from(Instant.now().plus(Duration.ofHours(1))),
                    QueryResponse.Source.ZOWE.value, Collections.emptyList(), QueryResponse.Source.ZOWE
                ));
            });
        }

        @BeforeEach
        void retryOnDroppedConnection() {
            HttpRequestRetryHandler retryHandler = (exception, executionCount, context) ->
                exception instanceof NoHttpResponseException && executionCount < 4;
            this.config = RestAssured.config;
            RestAssured.config = RestAssured.config
                .httpClient(HttpClientConfig.httpClientConfig().setParam("http.method.retry-handler", retryHandler));
        }

        @AfterEach
        void restoreConfig() {
            RestAssured.config = this.config;
        }

    }

    private String login(String user) {
        var now = Instant.now();
        var claims = new JWTClaimsSet.Builder()
            .subject(user)
            .issuer(QueryResponse.Source.ZOWE.value)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plus(Duration.ofHours(1))))
            .build();
        return new PlainJWT(claims).serialize();
    }

    @Nested
    @TestPropertySource(
        properties = {
            "server.ssl.keyStore=../keystore/localhost/localhost.keystore.p12",
            "server.ssl.trustStore=../keystore/localhost/localhost.truststore.p12",
            "apiml.security.auth.provider=dummy"
        }
    )
    @ActiveProfiles({"default", "test"})
    @AcceptanceTest
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class GivenDefaultProfile extends ActuatorAcceptanceTest {

        @Autowired
        private Environment environment;

        @BeforeEach
        void setUp() {
            var property = environment.getProperty("management.endpoints.web.exposure.include");
            assertEquals("health,info", property);
        }

        @ParameterizedTest
        @CsvSource({
            "/application/loggers",
            "/application/gateway"
        })
        void whenAccessDangerousActuatorWithoutCredentials_thenBlock(String endpoint) {
            given()
            .when()
                .get(basePath + endpoint)
            .then()
                .statusCode(SC_UNAUTHORIZED);
        }

        @ParameterizedTest
        @CsvSource({
            "/application/loggers",
            "/application/gateway"
        })
        void whenAccessDangerousActuatorWithCredentials_thenBlock(String endpoint) {
            given()
                .cookie(AUTH_COOKIE, login(USER))
            .when()
                .get(basePath + endpoint)
            .then()
                .log().ifValidationFails()
                .statusCode(SC_NOT_FOUND);
        }

    }

    @Nested
    @ActiveProfiles({"test", "debug"})
    @TestPropertySource(
        properties = {
            "server.ssl.keyStore=../keystore/localhost/localhost.keystore.p12",
            "server.ssl.trustStore=../keystore/localhost/localhost.truststore.p12",
            "apiml.security.auth.provider=dummy",
            "logging.level.reactor.netty=ERROR",
            "org.springframework.http.server.reactive=DEBUG",
            "org.springframework.security=DEBUG",
            "org.springframework.web.reactive=DEBUG",
            "org.springframework.web.reactive.socket=DEBUG"
        }
    )
    @AcceptanceTest
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class GivenDebugProfile extends ActuatorAcceptanceTest {

        @Autowired
        private Environment environment;

        @BeforeEach
        void setUp() {
            var exposure = environment.getProperty("management.endpoints.web.exposure.include");
            assertEquals("health,info,gateway,loggers", exposure);
            var gatewayAccess = environment.getProperty("management.endpoint.gateway.access");
            assertEquals("read-only", gatewayAccess);
            var loggersAccess = environment.getProperty("management.endpoint.loggers.access");
            assertEquals("read-only", loggersAccess);
        }

        @ParameterizedTest
        @CsvSource({
            "/application/loggers",
            "/application/gateway"
        })
        void whenAccessDangerousActuatorWithCredentials_thenBlockModify(String endpoint) {
            var jwt = login(USER);
            // change the level of the ROOT logger
            given()
                .cookie("apimlAuthenticationToken", jwt)
                .contentType("application/json")
                .body("{\"configuredLevel\":\"DEBUG\"}")
            .when()
                .post(basePath + "/application/loggers/ROOT")
            .then()
                .statusCode(SC_METHOD_NOT_ALLOWED);

            // refresh the gateway's routes
            given()
                .cookie("apimlAuthenticationToken", jwt)
            .when()
                .post(basePath + "/application/gateway/refresh")
            .then()
                .statusCode(SC_NOT_FOUND);
        }

        @ParameterizedTest
        @CsvSource({
            "/application/loggers",
            "/application/gateway"
        })
        void whenAccessDangerousActuatorWithoutCredentials_thenBlock(String endpoint) {
            given()
            .when()
                .get(basePath + endpoint)
            .then()
                .statusCode(SC_UNAUTHORIZED);
        }

        @ParameterizedTest
        @CsvSource({
            "/application/loggers",
            "/application/gateway/routes",
            "/application/info"
        })
        void whenAccessDangerousActuator_thenAllowRead(String endpoint) {
            given()
                .cookie(AUTH_COOKIE, login(USER))
            .when()
                .get(basePath + endpoint)
            .then()
                .statusCode(SC_OK);
        }

    }

    @Nested
    @ActiveProfiles({"test", "debug", "debug-control"})
    @TestPropertySource(
        properties = {
            "server.ssl.keyStore=../keystore/localhost/localhost.keystore.p12",
            "server.ssl.trustStore=../keystore/localhost/localhost.truststore.p12",
            "apiml.security.auth.provider=dummy",
            "logging.level.reactor.netty=ERROR",
            "org.springframework.http.server.reactive=DEBUG",
            "org.springframework.security=DEBUG",
            "org.springframework.web.reactive=DEBUG",
            "org.springframework.web.reactive.socket=DEBUG"
        }
    )
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @AcceptanceTest
    class GivenDebugControlProfile extends ActuatorAcceptanceTest {

        @Autowired
        private Environment environment;

        @BeforeEach
        void setUp() {
            var exposure = environment.getProperty("management.endpoints.web.exposure.include");
            assertEquals("health,info,gateway,loggers", exposure);
            var gatewayAccess = environment.getProperty("management.endpoint.gateway.access");
            assertEquals("unrestricted", gatewayAccess);
            var loggersAccess = environment.getProperty("management.endpoint.loggers.access");
            assertEquals("unrestricted", loggersAccess);
        }

        @Test
        void whenAccessDangerousActuatorWithCredentialsWithPermission_thenAllowModify() {
            var jwt = login(USER);
            // change the level of the ROOT logger
            given()
                .cookie("apimlAuthenticationToken", jwt)
                .contentType("application/json")
                .body("{\"configuredLevel\":\"DEBUG\"}")
            .when()
                .post(basePath + "/application/loggers/ROOT")
            .then()
                .statusCode(SC_NO_CONTENT);

            // refresh the gateway's routes
            given()
                .cookie("apimlAuthenticationToken", jwt)
            .when()
                .post(basePath + "/application/gateway/refresh")
            .then()
                .statusCode(SC_OK);
        }

        @Test
        void whenAccessDangerousActuatorWithCredentialsWithoutPermission_thenBlock() {
            var jwt = login(USER_NO_PERMISSION);
            String endpoint = "/application/loggers";
            // update a logger level
            given()
                .cookie("apimlAuthenticationToken", jwt)
            .when()
                .post(basePath + endpoint)
            .then()
                .statusCode(SC_FORBIDDEN);

            endpoint = "/application/gateway";
            // update routes
            given()
                .cookie("apimlAuthenticationToken", jwt)
            .when()
                .post(basePath + endpoint)
            .then()
                .statusCode(SC_FORBIDDEN);
        }

        @ParameterizedTest
        @CsvSource({
            "/application/loggers",
            "/application/gateway"
        })
        void whenAccessDangerousActuatorWithoutCredentials_thenBlock(String endpoint) {
            given()
            .when()
                .get(basePath + endpoint)
            .then()
                .statusCode(SC_UNAUTHORIZED);
        }

        @ParameterizedTest
        @CsvSource({
            "/application/loggers",
            "/application/gateway/routes"
        })
        void whenAccessDangerousActuator_thenAllowRead(String endpoint) {
            var jwt = login(USER_NO_PERMISSION);
            given()
                .cookie(AUTH_COOKIE, jwt)
            .when()
                .get(basePath + endpoint)
            .then()
                .statusCode(SC_OK);
        }

    }

}
