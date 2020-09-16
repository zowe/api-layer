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
import org.zowe.apiml.util.categories.MainframeDependentTests;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.gatewayservice.SecurityUtils.getConfiguredSslConfig;

@MainframeDependentTests
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

        given()
            .cookie(COOKIE_NAME, jwt1)
            .when()
            .post(SecurityUtils.getGateWayUrl(LOGOUT_ENDPOINT))
            .then()
            .statusCode(is(SC_NO_CONTENT));

        assertIfLogged(jwt1, false);
        assertIfLogged(jwt2, true);
    }

}
