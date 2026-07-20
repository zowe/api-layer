/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.acceptance;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.gateway.acceptance.common.AcceptanceTestWithBasePath;
import org.zowe.apiml.gateway.acceptance.common.MicroservicesAcceptanceTest;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.apache.hc.core5.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;
import static org.apache.hc.core5.http.HttpStatus.SC_UNAUTHORIZED;

@MicroservicesAcceptanceTest
@TestPropertySource(
    properties = {
        "spring.config.additional-location=file:../gateway-service/src/main/resources/application.yml",
        "server.ssl.keyStore=../keystore/localhost/localhost.keystore.p12",
        "server.ssl.trustStore=../keystore/localhost/localhost.truststore.p12",
        "apiml.security.auth.provider=dummy"
    }
)
@NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
class ActuatorConfigTest {

    private static final String USER = "USER";
    private static final String PASSWORD = "validPassword";

    private static final String USER_NO_PERMISSION = "APIMTST";

    private static final String LOGIN_ENDPOINT = "/gateway/api/v1/auth/login";
    private static final String AUTH_COOKIE = "apimlAuthenticationToken";

    @Nested
    class GivenDefaultProfile extends AcceptanceTestWithBasePath {

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
                .cookie(AUTH_COOKIE, login(basePath, USER))
            .when()
                .get(basePath + endpoint)
            .then()
                .statusCode(SC_UNAUTHORIZED);
        }

    }

    @Nested
    @ActiveProfiles("debug")
    class GivenDebugProfile extends AcceptanceTestWithBasePath {

        @ParameterizedTest
        @CsvSource({
            "/application/loggers",
            "/application/gateway"
        })
        void whenAccessDangerousActuatorWithCredentials_thenBlockModify(String endpoint) {
            var jwt = login(basePath, USER);
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
            "/application/gateway",
            "/application/info" // open without credentials?
        })
        void whenAccessDangerousActuator_thenAllowRead(String endpoint) {
            given()
                .cookie(AUTH_COOKIE, login(basePath, USER))
            .when()
                .get(basePath + endpoint)
            .then()
                .statusCode(SC_OK);
        }

    }

    @Nested
    @ActiveProfiles("debug-control")
    class GivenDebugControlProfile extends AcceptanceTestWithBasePath {

        @Test
        void whenAccessDangerousActuatorWithCredentialsWithPermission_thenAllowModify() {
            var jwt = login(basePath, USER);
            // update a logger level
            String endpoint = ""; // TODO
            given()
                .cookie("apimlAuthenticationToken", jwt)
            .when()
                .post(endpoint)
            .then()
                .statusCode(SC_OK);

            // update routes
            endpoint = ""; // TODO
            given()
                .cookie("apimlAuthenticationToken", jwt)
            .when()
                .post(endpoint)
            .then()
                .statusCode(SC_OK);
        }

        @Test
        void whenAccessDangerousActuatorWithCredentialsWithoutPermission_thenBlock() {
            var jwt = login(basePath, USER_NO_PERMISSION);
            String endpoint = "/"; // TODO
            // update a logger level
            given()
                .cookie("apimlAuthenticationToken", jwt)
            .when()
                .post(endpoint)
            .then()
                .statusCode(SC_FORBIDDEN);

            endpoint = ""; // TODO
            // update routes
            given()
                .cookie("apimlAuthenticationToken", jwt)
            .when()
                .post(endpoint)
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
            "/application/gateway"
        })
        void whenAccessDangerousActuator_thenAllowRead(String endpoint) {
            var jwt = login(basePath, USER_NO_PERMISSION);
            given()
                .cookie(AUTH_COOKIE, jwt)
            .when()
                .get(basePath + endpoint)
            .then()
                .statusCode(SC_OK);
        }

    }

    private String login(String basePath, String user) {
        var token = given()
            .contentType(ContentType.JSON)
            .body(String.format("""
                    {
                        "username": "%s",
                        "password": "%s"
                    }
                """, user, PASSWORD))
            .log().all()
        .when()
            .post(URI.create(basePath + LOGIN_ENDPOINT))
        .then()
            .statusCode(204)
            .cookie(AUTH_COOKIE)
        .extract()
            .cookie(AUTH_COOKIE);

        return token;
    }

}
