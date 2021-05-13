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
import org.zowe.apiml.util.categories.zOSMFAuthTest;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.zowe.apiml.util.SecurityUtils.*;

@zOSMFAuthTest
@SuppressWarnings({"squid:S2187"})
class ZosmfLogoutTest {

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
}
