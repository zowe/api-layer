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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.categories.SAFAuthTest;

import java.net.URI;

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
class SafLoginTest {
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
}
