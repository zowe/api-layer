/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.zowe.apiml.acceptance.config.ApimlRoutingConfig;
import org.zowe.apiml.acceptance.config.DiscoveryClientTestConfig;
import org.zowe.apiml.acceptance.config.GatewayOverrideConfig;
import org.zowe.apiml.acceptance.config.GatewayTestApplication;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

@SpringBootTest(classes = GatewayTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"apiml.health.protected=true"})
@Import({GatewayOverrideConfig.class, DiscoveryClientTestConfig.class, ApimlRoutingConfig.class})
public class ProtectedHealthEndpointTest {

    protected String basePath;

    @LocalServerPort
    protected int port;

    @BeforeEach
    public void setBasePath() {
        basePath = String.format("https://localhost:%d", port);
    }
    @Nested
    class GivenHealthEndPointProtectionEnabled {
        @Test
        void requestFailsWith401() {
            given()
                .when()
                .get(basePath  + "/application/health")
                .then()
                .statusCode(is(HttpStatus.SC_UNAUTHORIZED));
        }
    }
}
