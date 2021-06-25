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
import org.zowe.apiml.util.requests.JsonResponse;
import org.zowe.apiml.util.requests.ha.HAGatewayRequests;

import java.io.IOException;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Verify behaviour of the Gateway under chaotic testing
 */
@ChaoticHATest
public class GatewayChaoticTest {
    private final HAGatewayRequests haGatewayRequests = new HAGatewayRequests();

    private final String DISCOVERABLE_GREET = "/api/v1/discoverableclient/greeting";


    @Nested
    class GivenHASetUp {
        @Nested
        class whenOneGatewayIsNotAvailable {
            @Test
            void routeToInstanceThroughAliveGateway() throws IOException {
                assumeTrue(haGatewayRequests.existing() > 1);

                haGatewayRequests.shutdown(0);

                JsonResponse result = haGatewayRequests.route(1, DISCOVERABLE_GREET);
                assertThat(result.getStatus(), is(SC_OK));
            }
        }
    }

}
