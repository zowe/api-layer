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

import io.restassured.RestAssured;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.zowe.apiml.gateway.GatewayServiceApplication;
import org.zowe.apiml.gateway.acceptance.config.DiscoveryClientTestConfig;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

@SpringBootTest(classes = GatewayServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"apiml.health.protected=false"})
@Import(DiscoveryClientTestConfig.class)
public class ProtectedHealthEndpointTest {

    @Value("${apiml.service.hostname:localhost}")
    protected String hostname;

    @LocalServerPort
    protected int port;

    @BeforeEach
    void setUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    /*
     Service return 503 because of health indicator. Test validates only the access to the endpoint.
     */
    @Test
    void givenNoCredentials_thenReturnServiceUnavailable() {

        given()
            .when()
            .get(String.format("https://%s:%d/application/health", hostname, port))
            .then()
            .statusCode(is(HttpStatus.SC_SERVICE_UNAVAILABLE));
    }
}
