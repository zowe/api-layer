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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration;
import org.zowe.apiml.gateway.acceptance.common.AcceptanceTestWithBasePath;
import org.zowe.apiml.gateway.acceptance.common.MicroservicesAcceptanceTest;

import static io.restassured.RestAssured.given;
import static org.apache.hc.core5.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;

@MicroservicesAcceptanceTest
@TestPropertySource(
    properties = {
        "spring.config.additional-location=file:../gateway-service/src/main/resources/application.yml",
        "server.ssl.keyStore=../keystore/localhost/localhost.keystore.p12",
        "server.ssl.trustStore=../keystore/localhost/localhost.truststore.p12"
    }
)
@NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
class ActuatorConfigTest {

    @Nested
    class GivenDefaultProfile extends AcceptanceTestWithBasePath {

        @Test
        void whenAccessDangerousActuatorWithCredentials_thenBlock() {
            given()
            .when()
                .get(basePath + "/application/gateway")
            .then()
                .statusCode(SC_NOT_FOUND);

            given()
            .when()
                .get(basePath + "/application/loggers")
            .then()
                .statusCode(SC_NOT_FOUND);
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
            given()
            .when()
                .post(basePath + endpoint)
            .then()
                .statusCode(SC_OK);
        }

        @ParameterizedTest
        @CsvSource({
            "/application/loggers",
            "/application/gateway"
        })
        void whenAccessDangerousActuator_thenAllowRead(String endpoint) {
            given()
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
            given()
            .when()
            .then();
        }

        @Test
        void whenAccessDangerousActuatorWithCredentialsWithoutPermission_thenBlock() {
            // update a logger level
            given()
            .when()
            .then();

            // update routes
            given()
            .when()
            .then();
        }

        @ParameterizedTest
        @CsvSource({
            "/application/loggers",
            "/application/gateway"
        })
        void whenAccessDangerousActuatorWithoutCredentials_thenBlock() {
            given()
            .when()

            .then();
        }

        void whenAccessDangerousActuator_thenAllowRead() {

        }

    }

    private String login() {
        return null;
    }

}
