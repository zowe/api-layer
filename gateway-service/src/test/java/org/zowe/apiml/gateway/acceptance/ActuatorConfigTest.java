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
@TestPropertySource(properties = "spring.config.additional-location=file:gateway-service/src/main/resources/application.yml")
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

        @Test
        void whenAccessDangerousActuatorWithCredentials_thenBlockModify() {
            given()
            .when()
                .get(basePath + "/application/loggers")
            .then()
                .statusCode(SC_OK);

            given()
            .when()
            .then();
        }

    }

    @Nested
    @ActiveProfiles("debug-control")
    class GivenDebugControlProfile extends AcceptanceTestWithBasePath {

        @Test
        void whenAccessDangerousActuatorWithCredentials_thenAllowModify() {
            given()
            .when()
            .then();
        }

        @Test
        void whenAccessDangerousActuatorWithoutCredentials_thenBlock() {
            given()
            .when()
            .then();
        }

    }

}
