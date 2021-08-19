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
import org.zowe.apiml.util.categories.ChaoticHATest;
import org.zowe.apiml.util.requests.Endpoints;
import org.zowe.apiml.util.requests.JsonResponse;
import org.zowe.apiml.util.requests.ha.HADiscoverableClientRequests;
import org.zowe.apiml.util.requests.ha.HAGatewayRequests;

import java.util.List;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Verify behaviour of the Discoverable Client under chaotic testing
 * Test case: take down one of the DC instances and check whether the GW can route to the other alive DC instance
 */
@ChaoticHATest
public class SouthboundServiceChaoticTest {

    private final HAGatewayRequests haGatewayRequests = new HAGatewayRequests();
    private final HADiscoverableClientRequests haDiscoverableClientRequests = new HADiscoverableClientRequests();

    @Nested
    class GivenHASetUp {
        @Nested
        class whenOneDiscoverableClientIsNotAvailable {
            @Test
            void routeViaGatewayToTheOtherInstance() {
                assumeTrue(haDiscoverableClientRequests.existing() > 1);
//                haDiscoverableClientRequests.shutdown(0);
                routeToDiscoverableClient();
//                JsonResponse result = haGatewayRequests.route(1, Endpoints.DISCOVERABLE_GREET);
//                JsonResponse result2 = haGatewayRequests.route(0, Endpoints.DISCOVERABLE_GREET);
//                assertThat(result.getStatus(), is(SC_OK));
//                assertThat(result2.getStatus(), is(SC_OK));
                haDiscoverableClientRequests.shutdown(0);
                routeToDiscoverableClient();
            }

            @Test
            void routeViaGatewayToTheOtherInstance2() {
                assumeTrue(haDiscoverableClientRequests.existing() > 1);
//                haDiscoverableClientRequests.shutdown(0);
                routeToDiscoverableClient();
//                JsonResponse result = haGatewayRequests.route(1, Endpoints.DISCOVERABLE_GREET);
//                JsonResponse result2 = haGatewayRequests.route(0, Endpoints.DISCOVERABLE_GREET);
//                assertThat(result.getStatus(), is(SC_OK));
//                assertThat(result2.getStatus(), is(SC_OK));
                haDiscoverableClientRequests.shutdown(1);
                routeToDiscoverableClient();
            }

            private void routeToDiscoverableClient() {
                List<JsonResponse> responses = haGatewayRequests.route(Endpoints.DISCOVERABLE_GREET);
                for (JsonResponse response : responses)
                assertThat(response.getStatus(), is(SC_OK));
            }
        }
    }
}
