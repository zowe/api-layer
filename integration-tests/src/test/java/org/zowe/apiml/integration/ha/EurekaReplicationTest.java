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
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.HATest;
import org.zowe.apiml.util.requests.ha.HADiscoveryRequests;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Verify that eureka is aware of other replicas if they are online.
 */
@HATest
class EurekaReplicationTest implements TestWithStartedInstances {
    private HADiscoveryRequests haDiscoveryRequests = new HADiscoveryRequests();

    /**
     * It tests for registered instances.
     */
    @Nested
    class GivenMultipleEurekaInstances {
        @Nested
        class WhenLookingForEurekas {
            @Test
            void eurekaReplicasAreVisible() {
                assumeTrue(haDiscoveryRequests.existing() > 1);

                List<Integer> instances = haDiscoveryRequests.getAmountOfRegisteredInstancesForService("DISCOVERY");
                for(Integer registeredToInstance: instances) {
                    assertThat(registeredToInstance, is(haDiscoveryRequests.existing()));
                }
            }
        }
    }
}
