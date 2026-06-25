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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.TestPropertySource;

import java.util.Base64;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

@AcceptanceTest
@TestPropertySource(properties = {
    "apiml.security.ssl.verifySslCertificatesOfServices=false",
    "apiml.discovery.userid=user",
    "apiml.discovery.password=pass",
    ""
})
class EurekaAuthTest extends AcceptanceTestWithBasePath {


    static Stream<Arguments> inputs() {
        return Stream.of(Arguments.of("user:pass", 200), Arguments.of("invalid:password", 401), Arguments.of("", 401));
    }

    @ParameterizedTest
    @MethodSource("inputs")
    void givenNoCredentials_whenGetEureka_thenReturn401(String credentials, int statusCode) {
        given()
            .when()
            .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes()))
            .get("https://localhost:" + DISCOVERY_PORT + "/eureka/apps")
            .then()
            .statusCode(statusCode);
    }

}
