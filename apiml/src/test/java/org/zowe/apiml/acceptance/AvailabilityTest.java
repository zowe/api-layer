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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.i18n.FixedLocaleContextResolver;
import org.springframework.web.server.i18n.LocaleContextResolver;

import static io.restassured.RestAssured.given;

/**
 * This test requires port 10011 available for DS port test
 */
@AcceptanceTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles({ "ApimlModulithAcceptanceTest", "AvailabilityTest" })
public class AvailabilityTest extends AcceptanceTestWithBasePath {

    @Test
    void gatewayIsAvailable() {
        given()
        .when()
            .get("https://localhost:" + port)
        .then()
            .log().all()
            .statusCode(200);
    }

    @Test
    void discoveryIsAvailable() {
        given()
        .when()
            .get("https://localhost:10011")
        .then()
            .statusCode(401);
    }

    @Profile("AvailabilityTest")
    @TestConfiguration
    public static class TestConfig {

        @Bean
        LocaleContextResolver localeContextResolver() {
            return new FixedLocaleContextResolver();
        }

    }

}
