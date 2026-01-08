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

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.i18n.FixedLocaleContextResolver;
import org.springframework.web.server.i18n.LocaleContextResolver;

import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;

/**
 * This test requires port 10011 available for DS port test
 */
@AcceptanceTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles({ "ApimlModulithAcceptanceTest", "AvailabilityTest" })
public class AvailabilityTest extends AcceptanceTestWithBasePath {

    @ParameterizedTest(name = "{0} is available at port {1} with status {2}")
    @CsvSource({
        "Gateway, 0, 200",
        "Discovery, 10011, 401"
    })
    void serviceIsAvailable(String serviceName, int servicePort, int expectedStatus) {
        int actualPort = servicePort == 0 ? port : servicePort;
        await().atMost(30, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .ignoreExceptions()
            .untilAsserted(() ->
                given()
                .when()
                    .get("https://localhost:" + actualPort)
                .then()
                    .statusCode(expectedStatus)
            );
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
