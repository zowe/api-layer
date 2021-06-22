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
 * Verify behaviour of the application under chaotic testing
 */
@ChaoticHATest
public class ChaoticTest {

    private GatewayServiceConfiguration gatewayServiceConfiguration;

    private final String DISCOVERABLE_GREET = "/api/v1/discoverableclient/greeting";
    private final String GATEWAY_SHUTDOWN = "/application/shutdown";

    private String username;
    private String password;
    private String discoverableClientPort;
    private String discoverableClientHost;

    @BeforeEach
    void setUp() {
        gatewayServiceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        username = ConfigReader.environmentConfiguration().getCredentials().getUser();
        password = ConfigReader.environmentConfiguration().getCredentials().getPassword();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
    }

    @Nested
    class GivenHASetUp {
        @Nested
        class whenOneGatewayIsNotAvailable {
            @Test
            void routeToTheAliveGatewayInstance() throws IOException {
                final int instances = gatewayServiceConfiguration.getInstances();
                assumeTrue(instances > 1);
                String[] hosts = gatewayServiceConfiguration.getHost().split(",");
                shutDownGatewayInstance(hosts[0]);
                int port = gatewayServiceConfiguration.getPort();
                HttpRequestUtils.getResponse(DISCOVERABLE_GREET, SC_OK, port, hosts[1]);
            }

            void shutDownGatewayInstance(String host) {
                //@formatter:off
                given()
                    .contentType(JSON)
                    .auth().basic(username, password)
                    .when()
                    .post(HttpRequestUtils.getUriFromGateway(GATEWAY_SHUTDOWN, gatewayServiceConfiguration.getPort(), host, Collections.emptyList()))
                    .then()
                    .statusCode(is(SC_OK))
                    .extract().body().asString();
                //@formatter:on
            }
        }

//        @Nested
//        class whenOneDiscoveryServiceIsNotAvailable {
//            @Test
//            void routeToTheAliveGatewayInstance() throws IOException {
//                final int instances = gatewayServiceConfiguration.getInstances();
//                assumeTrue(instances > 1);
//                String[] hosts = gatewayServiceConfiguration.getHost().split(",");
//                shutDownGatewayInstance(hosts[0]);
//                int port = gatewayServiceConfiguration.getPort();
//                //@formatter:off
//                HttpRequestUtils.getResponse(DISCOVERABLE_GREET, SC_OK, port, hosts[1]);
//            }
//
//            void shutDownGatewayInstance(String host) {
//                //@formatter:off
//                given()
//                    .auth().basic(username, password)
//                    .when()
//                    .post(HttpRequestUtils.getUriFromGateway(GATEWAY_SHUTDOWN, gatewayServiceConfiguration.getPort(), host, Collections.emptyList()))
//                    .then()
//                    .statusCode(is(SC_OK))
//                    .extract().body().asString();
//                //@formatter:on
//            }
//        }
    }

}
