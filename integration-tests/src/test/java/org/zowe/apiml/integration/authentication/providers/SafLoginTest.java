/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.authentication.providers;

import io.restassured.RestAssured;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.NotForMainframeTest;
import org.zowe.apiml.util.categories.SAFAuthTest;
import org.zowe.apiml.util.config.ConfigReader;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

/**
 * Test that when valid credentials are provided the SAF authentication provider will accept them and the valid token
 * will be produced.
 * <p>
 * Also verify that the invalid credentials will be properly rejected.
 */
@SAFAuthTest
@Tag("SAFProviderTest")
class SafLoginTest implements TestWithStartedInstances {
    @BeforeAll
    static void switchToTestedProvider() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Nested
    class WhenUserAuthenticatesTwice {
        @Nested
        class ReturnTwoDifferentValidTokens {
            @ParameterizedTest(name = "givenValidCredentialsInBody {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.authentication.providers.LoginTest#loginUrlsSource")
            void givenValidCredentialsInBody(URI loginUrl) {
                String jwtToken1 = SecurityUtils.gatewayToken(loginUrl);
                String jwtToken2 = SecurityUtils.gatewayToken(loginUrl);

                assertThat(jwtToken1, is(not(jwtToken2)));
            }
        }
    }

    @Nested
    @NotForMainframeTest
    class ExpiredPassword {

        private final String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
        private final String EXPIRED_PASSWORD = "expiredPassword";

        @ParameterizedTest(name = "givenExpiredAccountCredentialsInBody {index} {0} ")
        @MethodSource("org.zowe.apiml.integration.authentication.providers.LoginTest#loginUrlsSource")
        void givenExpiredAccountCredentialsInBody(URI loginUrl) {
            LoginRequest loginRequest = new LoginRequest(USERNAME, EXPIRED_PASSWORD.toCharArray());

            given()
                .contentType(JSON)
                .body(loginRequest)
            .when()
                .post(loginUrl)
            .then()
                .statusCode(is(SC_UNAUTHORIZED))
                .body(
                    "messages.find { it.messageNumber == 'ZWEAT412E' }.messageContent", containsString("expire")
                );
        }

        @ParameterizedTest(name = "givenExpiredAccountCredentialsInHeader {index} {0} ")
        @MethodSource("org.zowe.apiml.integration.authentication.providers.LoginTest#loginUrlsSource")
        void givenExpiredAccountCredentialsInHeader(URI loginUrl) {
            String headerValue = "Basic " + Base64.getEncoder().encodeToString((USERNAME + ":" + EXPIRED_PASSWORD).getBytes(StandardCharsets.UTF_8));

            given()
                .contentType(JSON)
                .header(HttpHeaders.AUTHORIZATION, headerValue)
            .when()
                .post(loginUrl)
            .then()
                .statusCode(is(SC_UNAUTHORIZED))
                .body(
                    "messages.find { it.messageNumber == 'ZWEAT412E' }.messageContent", containsString("expire")
                );
        }

    }

}
