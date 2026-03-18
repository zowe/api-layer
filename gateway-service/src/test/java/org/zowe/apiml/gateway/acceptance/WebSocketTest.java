/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.acceptance;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.gateway.MockService;
import org.zowe.apiml.gateway.acceptance.common.AcceptanceTestWithMockServices;
import org.zowe.apiml.gateway.acceptance.common.MicroservicesAcceptanceTest;

@MicroservicesAcceptanceTest
@TestInstance(Lifecycle.PER_CLASS)
@ActiveProfiles({ "WebSocketTest" })
@TestPropertySource(
    properties = {
        "=1000"
    }
)
class WebSocketTest extends AcceptanceTestWithMockServices {

    private MockService mockServiceWs;

    @BeforeAll
    void setUp() {
        // mockServiceWs = mockServiceWs("wsservice")
        // .addEndpoint("basePath")
        // .assertions(List<Consumer<HttpExchange>>.of())
        // .and().start();
    }

    @Test
    void test() {

    }

}

@TestConfiguration
@Profile("WebSocketTest")
class WebSocketTestConfiguration {

}
