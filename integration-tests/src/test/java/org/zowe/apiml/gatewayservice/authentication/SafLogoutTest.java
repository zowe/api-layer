/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gatewayservice.authentication;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.gatewayservice.SecurityUtils;
import org.zowe.apiml.util.categories.AuthenticationTest;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.zowe.apiml.gatewayservice.SecurityUtils.getConfiguredSslConfig;

@AuthenticationTest
class SafLogoutTest extends LogoutTest {
    private static AuthenticationProviders providers = new AuthenticationProviders(SecurityUtils.getGateWayUrl("/authentication"));

    // Change to saf and run the same test as for the zOSMF
    @BeforeAll
    static void switchToTestedProvider() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());

        providers.switchProvider("saf");
    }

    @Test
    void givenTwoValidTokens_whenLogoutCalledOnFirstOne_thenSecondStillValid() {
        String jwt1 = generateToken();
        String jwt2 = generateToken();

        assertIfLogged(jwt1, true);
        assertIfLogged(jwt2, true);

        assertLogout(jwt1, SC_NO_CONTENT);

        assertIfLogged(jwt1, false);
        assertIfLogged(jwt2, true);

        logout(jwt2);
    }

    @Test
    void givenValidToken_whenLogoutCalledTwice_thenSecondCallUnauthorized() {
        String jwt = generateToken();

        assertIfLogged(jwt, true);

        assertLogout(jwt, SC_NO_CONTENT);
        assertLogout(jwt, SC_UNAUTHORIZED);
    }
}
