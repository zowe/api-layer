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
import org.zowe.apiml.util.requests.Endpoints;
import org.zowe.apiml.util.requests.JsonResponse;
import org.zowe.apiml.util.requests.ha.HAGatewayRequests;

import java.util.List;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Verify that a southbound service can route through multiples Gateway instances
 */
@HATest
public class SouthboundServicesRoutingTest {
    private final HAGatewayRequests haGatewayRequests = new HAGatewayRequests();

    @Nested
    class GivenHASetUp {
        @Nested
        class WhenCallingDiscoverableClient {
            @Test
            void routeThroughEveryInstance() {
                assumeTrue(haGatewayRequests.existing() > 1);

                List<JsonResponse> responses = haGatewayRequests.route(Endpoints.DISCOVERABLE_GREET);
                for (JsonResponse response : responses) {
                    assertThat(response.getStatus(), is(SC_OK));
                }
            }
        }
    }
}
