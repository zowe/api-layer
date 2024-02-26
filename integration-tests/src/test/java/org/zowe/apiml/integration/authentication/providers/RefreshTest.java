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
import org.junit.jupiter.api.*;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.*;
import org.zowe.apiml.util.config.ItSslConfigFactory;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.requests.GatewayRequests;

import static org.zowe.apiml.util.SecurityUtils.assertIfLogged;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;

@GeneralAuthenticationTest
@SAFAuthTest
@zOSMFAuthTest
@TestsNotMeantForZowe
public class RefreshTest implements TestWithStartedInstances {
    private final GatewayRequests requests = new GatewayRequests();

    @BeforeAll
    public static void init() throws Exception {
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Nested
    class GivenLegalAccessModes {

        @Test
        void whenJwtTokenPostedCanBeRefreshedAndOldCookieInvalidated() {
            String validToken = requests.login();
            // The SSL context provides needed certificate to verify the claim
            String newToken = requests.refresh(validToken);

            assertIfLogged(validToken, false);
            assertIfLogged(newToken, true);
        }

    }

}
