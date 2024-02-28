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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.SAFAuthTest;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.zowe.apiml.util.SecurityUtils.*;

@SAFAuthTest
@Tag("SAFProviderTest")
class SafLogoutTest implements TestWithStartedInstances {

    // Change to saf and run the same test as for the zOSMF
    @BeforeAll
    static void switchToTestedProvider() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
    }

    @Nested
    class WhenUserLogsOutTwice {
        @Nested
        class SecondCallReturnUnauthorized {
            @ParameterizedTest(name = "givenValidToken {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.authentication.providers.LogoutTest#logoutUrlsSource")
            void givenValidToken(String logoutUrl) {
                String jwt = gatewayToken();

                assertIfLogged(jwt, true);

                assertLogout(logoutUrl, jwt, SC_NO_CONTENT);
                assertLogout(logoutUrl, jwt, SC_UNAUTHORIZED);
            }
        }
    }

    @Nested
    class WhenUserLogsOutOnceWithMultipleTokens {
        @Nested
        class VerifySecondTokenIsValid {
            @ParameterizedTest(name = "givenTwoValidTokens {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.authentication.providers.LogoutTest#logoutUrlsSource")
            void givenTwoValidTokens(String logoutUrl) {
                String jwt1 = gatewayToken();
                String jwt2 = gatewayToken();

                assertIfLogged(jwt1, true);
                assertIfLogged(jwt2, true);

                assertLogout(logoutUrl, jwt1, SC_NO_CONTENT);

                assertIfLogged(jwt1, false);
                assertIfLogged(jwt2, true);

                logoutOnGateway(logoutUrl, jwt2);
            }
        }
    }
}
