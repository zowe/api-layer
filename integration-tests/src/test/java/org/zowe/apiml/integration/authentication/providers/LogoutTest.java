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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.GeneralAuthenticationTest;
import org.zowe.apiml.util.categories.SAFAuthTest;
import org.zowe.apiml.util.categories.zOSMFAuthTest;

import static org.zowe.apiml.util.SecurityUtils.assertIfLogged;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;

/**
 * Basic set of logout related tests that needs to pass against every valid authentication provider.
 */
@GeneralAuthenticationTest
@SAFAuthTest
@zOSMFAuthTest
class LogoutTest implements TestWithStartedInstances {

    @BeforeEach
    void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
    }

    @Nested
    class WhenUserLogOut {
        @Nested
        class InvalidateTheToken {
            @Test
            void givenValidCredentials() {
                // make login
                String jwt = SecurityUtils.gatewayToken();

                // check if it is logged in
                assertIfLogged(jwt, true);

                SecurityUtils.logoutOnGateway(SecurityUtils.getGatewayLogoutUrl(), jwt);

                // check if it is logged out
                assertIfLogged(jwt, false);
            }
        }
    }
}
