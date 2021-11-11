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
import org.zowe.apiml.util.requests.Apps;
import org.zowe.apiml.util.requests.ha.HADiscoveryRequests;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Verify behaviour of the Discovery Service under chaotic testing
 */
@ChaoticHATest
public class DiscoveryChaoticTest {
    private final HADiscoveryRequests haDiscoveryRequests = new HADiscoveryRequests();

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
    }

}
