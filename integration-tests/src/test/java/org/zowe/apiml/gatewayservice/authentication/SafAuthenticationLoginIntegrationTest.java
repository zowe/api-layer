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
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.util.categories.AuthenticationTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

/**
 * Test that when valid credentials are provided the SAF authentication provider will accept them and the valid token
 * will be produced.
 * <p>
 * Also verify that the invalid credentials will be properly rejected.
 */
@AuthenticationTest
class SafAuthenticationLoginIntegrationTest extends Login {
    @BeforeAll
    static void switchToTestedProvider() {
        RestAssured.port = PORT;
        RestAssured.useRelaxedHTTPSValidation();

        providers.switchProvider("saf");
    }

    @Test
    void givenValidCredentialsInBody_whenUserAuthenticatesTwice_thenTwoDifferentValidTokenIsProduced() {
        LoginRequest loginRequest = new LoginRequest(getUsername(), getPassword());

        String jwtToken1 = authenticateAndVerify(loginRequest);
        String jwtToken2 = authenticateAndVerify(loginRequest);

        assertThat(jwtToken1, is(not(jwtToken2)));
    }


}
