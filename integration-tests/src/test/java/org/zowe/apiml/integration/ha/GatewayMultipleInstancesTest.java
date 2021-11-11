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
import org.zowe.apiml.util.categories.HATest;
import org.zowe.apiml.util.requests.Apps;
import org.zowe.apiml.util.requests.ha.HADiscoveryRequests;
import org.zowe.apiml.util.requests.ha.HAGatewayRequests;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;

/**
 * Verify that both Gateway instances are UP
 */
@HATest
public class GatewayMultipleInstancesTest {
    private final HAGatewayRequests haGatewayRequests = new HAGatewayRequests();
    private final HADiscoveryRequests haDiscoveryRequests = new HADiscoveryRequests();

    @BeforeEach
    void setUp() {
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
    }

    @Nested
    class GivenMultipleGatewayInstances {
        @Nested
        class WhenSendingRequest {
            @Test
            void gatewayInstancesAreUp() {
                assumeTrue(haGatewayRequests.existing() > 1);

                assertThat(haGatewayRequests.up(), is(true));
            }

            @Test
            void gatewayInstancesAreRegistered() {
                assumeTrue(haGatewayRequests.existing() > 1 && haDiscoveryRequests.existing() > 1);

                assertThat(haDiscoveryRequests.getAmountOfRegisteredInstancesForService(0, Apps.GATEWAY), is(2));
            }
        }
    }
}
