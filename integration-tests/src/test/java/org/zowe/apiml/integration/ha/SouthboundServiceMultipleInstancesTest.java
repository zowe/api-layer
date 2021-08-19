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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.categories.HATest;
import org.zowe.apiml.util.requests.Apps;
import org.zowe.apiml.util.requests.Endpoints;
import org.zowe.apiml.util.requests.JsonResponse;
import org.zowe.apiml.util.requests.ha.HADiscoverableClientRequests;
import org.zowe.apiml.util.requests.ha.HADiscoveryRequests;
import org.zowe.apiml.util.requests.ha.HAGatewayRequests;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Verify that both southbound (Discoverable Client) instances are UP and that the service can be accessible via Gateway
 */
@HATest
public class SouthboundServiceMultipleInstancesTest {
    private final HAGatewayRequests haGatewayRequests = new HAGatewayRequests();
    private final HADiscoverableClientRequests haDiscoverableClientRequests = new HADiscoverableClientRequests();
    private final HADiscoveryRequests haDiscoveryRequests = new HADiscoveryRequests();
    @Nested
    class GivenMultipleDiscoverableClientInstances {
        @Nested
        class WhenSendingRequest {

            @Test
            void discoverableClientInstancesAreUp() {
                assumeTrue(haDiscoverableClientRequests.existing() > 1);

                assertThat(haDiscoverableClientRequests.up(), is(true));
            }

            @Test
            void discoverableClientInstancesAreRegistered() {
                assumeTrue(haDiscoverableClientRequests.existing() > 1 && haDiscoveryRequests.existing() > 1);

                assertThat(haDiscoveryRequests.getAmountOfRegisteredInstancesForService(0, Apps.DISCOVERABLE_CLIENT), is(2));
            }

            @Test
            void discoverableClientIsAccessibleViaGateway() {
                assumeTrue(haDiscoverableClientRequests.existing() > 1);

                JsonResponse response = haGatewayRequests.route(0, Endpoints.DISCOVERABLE_GREET);
                assertThat(response.getStatus(), is(SC_OK));
            }
        }
    }
}
