/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.config;

import com.netflix.eureka.cluster.PeerEurekaNodes;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.discovery.ApimlInstanceRegistry;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class EurekaConfigTest {

    @Nested
    @SpringBootTest
    class Initialization {

        @Autowired
        private PeerEurekaNodes peerEurekaNodes;

        @Autowired
        private ApimlInstanceRegistry apimlInstanceRegistry;

        @Test
        void givenDefaultConfiguration_whenInitialize_thenPeerEurekaNodesIsAvailable() {
            assertNotNull(peerEurekaNodes);
        }

        @Test
        void givenDefaultConfiguration_whenInitialize_thenApimlInstanceRegistryIsFullyConfigured() {
            assertNotNull(apimlInstanceRegistry);
            var setPeerEurekaNodes = ReflectionTestUtils.getField(apimlInstanceRegistry, "peerEurekaNodes");
            assertNotNull(setPeerEurekaNodes);
            assertSame(peerEurekaNodes, setPeerEurekaNodes);
        }

    }

}
