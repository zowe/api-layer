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
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.categories.ChaoticHATest;
import org.zowe.apiml.util.requests.Apps;
import org.zowe.apiml.util.requests.Endpoints;
import org.zowe.apiml.util.requests.JsonResponse;
import org.zowe.apiml.util.requests.ha.HADiscoveryRequests;
import org.zowe.apiml.util.requests.ha.HAGatewayRequests;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;

/**
 * Verify behaviour of the application under chaotic testing
 */
@ChaoticHATest
public class ChaoticTest {
    private final HADiscoveryRequests haDiscoveryRequests = new HADiscoveryRequests();
    private final HAGatewayRequests haGatewayRequests = new HAGatewayRequests();

    @BeforeEach
    void setUp() {
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
    }

    @Nested
    class GivenHASetUp {
        @Nested
        class whenOneDiscoveryServiceIsNotAvailable {
            @Test
            void serviceStillRegisteredToOtherDiscovery() {
                assumeTrue(haDiscoveryRequests.existing() > 1);

                haDiscoveryRequests.shutdown(0);

                assertThat(haDiscoveryRequests.isApplicationRegistered(1, Apps.DISCOVERABLE_CLIENT), is(true));
            }
        }

        @Nested
        class whenOneGatewayIsNotAvailable {
            @Test
            void routeToInstanceThroughAliveGateway() {
                assumeTrue(haGatewayRequests.existing() > 1 && haDiscoveryRequests.existing() > 1);

                haGatewayRequests.shutdown(0);

                JsonResponse response = haGatewayRequests.route(1, Endpoints.DISCOVERABLE_GREET);
                assertThat(response.getStatus(), is(SC_OK));
            }
        }
    }

}
