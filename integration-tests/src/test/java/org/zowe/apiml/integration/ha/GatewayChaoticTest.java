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

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.categories.ChaoticHATest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.DiscoveryServiceConfiguration;
import org.zowe.apiml.util.config.EnvironmentConfiguration;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.io.IOException;
import java.util.Collections;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;

/**
 * Verify behaviour of the Gateway under chaotic testing
 */
@ChaoticHATest
public class GatewayChaoticTest {

    private GatewayServiceConfiguration gatewayServiceConfiguration;
    private DiscoveryServiceConfiguration discoveryServiceConfiguration;

    private final String DISCOVERABLE_GREET = "/api/v1/discoverableclient/greeting";
    private final String SHUTDOWN = "/application/shutdown";
    private int gatewayInstances;
    private int discoveryInstances;
    private String username;
    private String password;
    private String[] gatewayHosts;

    @BeforeEach
    void setUp() {
        EnvironmentConfiguration environmentConfiguration = ConfigReader.environmentConfiguration();
        gatewayServiceConfiguration = environmentConfiguration.getGatewayServiceConfiguration();
        discoveryServiceConfiguration = environmentConfiguration.getDiscoveryServiceConfiguration();
        username = environmentConfiguration.getCredentials().getUser();
        password = environmentConfiguration.getCredentials().getPassword();
        gatewayHosts = gatewayServiceConfiguration.getHost().split(",");
        gatewayInstances = gatewayServiceConfiguration.getInstances();
        discoveryInstances = discoveryServiceConfiguration.getInstances();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
    }

    @Nested
    class GivenHASetUp {
        @Nested
        class whenOneGatewayIsNotAvailable {
            @Test
            void routeToInstanceThroughAliveGateway() throws IOException {
                assumeTrue(gatewayInstances > 1 && discoveryInstances > 1);
                shutDownGatewayInstance(gatewayHosts[0]);
                int port = gatewayServiceConfiguration.getPort();
                HttpRequestUtils.getResponse(DISCOVERABLE_GREET, SC_OK, port, gatewayHosts[1]);
            }

            void shutDownGatewayInstance(String host) {
                //@formatter:off
                given()
                    .contentType(JSON)
                    .auth().basic(username, password)
                    .when()
                    .post(HttpRequestUtils.getUriFromGateway(SHUTDOWN, gatewayServiceConfiguration.getPort(), host, Collections.emptyList()))
                    .then()
                    .statusCode(is(SC_OK))
                    .extract().body().asString();
                //@formatter:on
            }
        }
    }

}
