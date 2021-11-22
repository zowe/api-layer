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
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.SAFAuthTest;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.zowe.apiml.util.requests.Endpoints.*;

/**
 * Test that when valid credentials are provided the SAF authentication provider will accept them and the valid token
 * will be produced.
 * <p>
 * Also verify that the invalid credentials will be properly rejected.
 */
@SAFAuthTest
class SafLoginTest implements TestWithStartedInstances {
    private URI LOGIN_ENDPOINT_URL = HttpRequestUtils.getUriFromGateway(ROUTED_LOGIN);

    @BeforeAll
    static void switchToTestedProvider() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Nested
    class WhenUserAuthenticatesTwice {
        @Nested
        class ReturnTwoDifferentValidTokens {
            @Test
            void givenValidCredentialsInBody() {
                String jwtToken1 = SecurityUtils.gatewayToken(LOGIN_ENDPOINT_URL);
                String jwtToken2 = SecurityUtils.gatewayToken(LOGIN_ENDPOINT_URL);

                assertThat(jwtToken1, is(not(jwtToken2)));
            }
        }
    }
}
