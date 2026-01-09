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
import org.zowe.apiml.util.config.ZaasConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.zowe.apiml.util.SecurityUtils.assertIfLogged;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;
import static org.zowe.apiml.util.config.ConfigReader.IS_MODULITH_ENABLED;
import static org.zowe.apiml.util.http.HttpRequestUtils.getUriFromService;
import static org.zowe.apiml.util.requests.Endpoints.ROUTED_LOGOUT;

/**
 * In initial version, basic logout test to verify token invalidation in HA scenarios
 */
@HATest
@Tag("SAFProviderTest")
class AuthenticationHaTest {

    private static final String ZAAS_QUERY = "/zaas/api/v1/auth/query";
    private static final GatewayServiceConfiguration GATEWAY_CONF = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
    private static final ZaasConfiguration ZAAS_CONF = ConfigReader.environmentConfiguration().getZaasConfiguration();
    private List<Throwable> errors;

    @BeforeEach
    void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        errors = new ArrayList<>();
    }

    @Nested
    class GivenMultipleInstances {

        @Nested
        class WhenUserLogOut {

            @Test
            void thenTokenIsInvalidatedInBoth() {
                var jwt = SecurityUtils.gatewayToken();
                var gatewayHosts = getGatewayHosts();

                assertIfLogged(jwt, true);

                // Logout on any instance
                SecurityUtils.logoutOnGateway(SecurityUtils.getGatewayUrl(gatewayHosts[0], ROUTED_LOGOUT), jwt);

                // Verify token is invalid in one or more Gateway and ZAAS instances. Do this twice
                for (int i = 0; i < 2; i++) {
                    assertIfGatewayLogged(jwt, false, gatewayHosts[0]);
                    if (!(IS_MODULITH_ENABLED || ZAAS_CONF == null)) {
                        assertIfZaasLogged(jwt, false, ZAAS_CONF);
                    }

                    assertIfGatewayLogged(jwt, false, gatewayHosts[1]);
                    if (!(IS_MODULITH_ENABLED || ZAAS_CONF == null)) {
                        String zaasHost = ZAAS_CONF.getAdditionalHost() != null ? ZAAS_CONF.getAdditionalHost() : ZAAS_CONF.getHost() + "-2";
                        // Since we have only one ZAAS instance in the configuration, manually add the second one
                        assertIfZaasLogged(jwt, false, new ZaasConfiguration(ZAAS_CONF.getScheme(), zaasHost, zaasHost, ZAAS_CONF.getPort(), 2));
                    }
                }

                assertTrue(
                    errors.isEmpty(),
                    () -> "Errors:\n" + errors.stream()
                        .map(Throwable::getMessage)
                        .collect(Collectors.joining("\n"))
                );
            }
        }
    }

    private void assertIfGatewayLogged(String jwt, boolean logged, String gatewayHost) {
        try {
            SecurityUtils.assertIfLogged(jwt, logged, gatewayHost);
        } catch (Throwable error) {
            errors.add(new Throwable(gatewayHost, error));
        }
    }

    private void assertIfZaasLogged(String jwt, boolean logged, ZaasConfiguration zaasConfiguration) {
        try {
            SecurityUtils.assertIfLogged(jwt, logged, getUriFromService(zaasConfiguration, ZAAS_QUERY));
        } catch (Throwable error) {
            errors.add(new Throwable(zaasConfiguration.getHost(), error));
        }
    }

    // assume only two gateway (or apiml) instances
    private String[] getGatewayHosts() {
        return GATEWAY_CONF.getHost().split(",");
    }

}
