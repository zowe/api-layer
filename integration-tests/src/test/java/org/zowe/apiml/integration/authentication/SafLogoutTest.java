/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.integration.authentication;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.util.categories.SAFAuthTest;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;

@SAFAuthTest
class SafLogoutTest extends LogoutTest {

    // Change to saf and run the same test as for the zOSMF
    @BeforeAll
    static void switchToTestedProvider() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
    }

    @ParameterizedTest
    @MethodSource("logoutUrlsSource")
    void givenTwoValidTokens_whenLogoutCalledOnFirstOne_thenSecondStillValid(String logoutUrl) {
        String jwt1 = generateToken();
        String jwt2 = generateToken();

        assertIfLogged(jwt1, true);
        assertIfLogged(jwt2, true);

        assertLogout(logoutUrl, jwt1, SC_NO_CONTENT);

        assertIfLogged(jwt1, false);
        assertIfLogged(jwt2, true);

        logout(logoutUrl, jwt2);
    }

    @ParameterizedTest
    @MethodSource("logoutUrlsSource")
    void givenValidToken_whenLogoutCalledTwice_thenSecondCallUnauthorized(String logoutUrl) {
        String jwt = generateToken();

        assertIfLogged(jwt, true);

        assertLogout(logoutUrl, jwt, SC_NO_CONTENT);
        assertLogout(logoutUrl, jwt, SC_UNAUTHORIZED);
    }
}
