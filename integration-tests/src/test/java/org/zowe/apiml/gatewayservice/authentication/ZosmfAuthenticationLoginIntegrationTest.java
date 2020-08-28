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
import org.zowe.apiml.util.categories.MainframeDependentTests;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@MainframeDependentTests
class ZosmfAuthenticationLoginIntegrationTest extends Login {
    @BeforeAll
    static void switchToTestedProvider() {
        RestAssured.port = PORT;
        RestAssured.useRelaxedHTTPSValidation();

        providers.switchProvider("zosmf");
    }

    /**
     * This is how z/OSMF behaves. Two logins with basic auth give identical token.
     */
    @Test
    void givenValidCredentialsInBody_whenUserAuthenticatesTwice_thenIdenticalTokenIsProduced() {
        LoginRequest loginRequest = new LoginRequest(getUsername(), getPassword());

        String jwtToken1 = authenticateAndVerify(loginRequest);
        String jwtToken2 = authenticateAndVerify(loginRequest);

        assertThat(jwtToken1, is((jwtToken2)));
    }
}
