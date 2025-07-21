/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.ha;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.categories.HATest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;

import static org.zowe.apiml.util.SecurityUtils.assertIfLogged;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;
import static org.zowe.apiml.util.requests.Endpoints.ROUTED_LOGOUT;

/**
 * In initial version, basic logout test to verify token invalidation in HA scenarios
 *
 */
@HATest
@Tag("SAFProviderTest")
class AuthenticationHaTest {

    private static final GatewayServiceConfiguration GATEWAY_CONF = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();

    @BeforeEach
    void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
    }

    @Nested
    class GivenMultipleInstances {

        @Nested
        class WhenUserLogOut {

            @Test
            void thenTokenIsInvalidatedInBoth() {
                var jwt = SecurityUtils.gatewayToken();

                assertIfLogged(jwt, true);

                // Logout on any instance
                SecurityUtils.logoutOnGateway(SecurityUtils.getGatewayUrl(getGatewayHosts()[0], ROUTED_LOGOUT), jwt);

                // Verify token is invalid in one or more Gateway instances
                var gatewayHosts = getGatewayHosts();
                for (String host : gatewayHosts) {
                    SecurityUtils.assertIfLogged(jwt, false, host);
                }

            }

        }

    }

    // assume only two gateway (or apiml) instances
    private String[] getGatewayHosts() {
        return GATEWAY_CONF.getHost().split(",");
    }

}
