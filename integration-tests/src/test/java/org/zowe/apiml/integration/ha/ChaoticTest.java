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
import org.zowe.apiml.util.DiscoveryRequests;
import org.zowe.apiml.util.categories.ChaoticHATest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.DiscoveryServiceConfiguration;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;

/**
 * Verify behaviour of the application under chaotic testing
 */
@ChaoticHATest
public class ChaoticTest {

    private GatewayServiceConfiguration gatewayServiceConfiguration;
    private DiscoveryServiceConfiguration discoveryServiceConfiguration;

    private final String DISCOVERABLE_GREET = "/api/v1/discoverableclient/greeting";
    private final String SHUTDOWN = "/application/shutdown";
    private DiscoveryRequests discoveryRequests;
    private String username;
    private String password;
    private String[] hosts;

    @BeforeEach
    void setUp() {
        gatewayServiceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        discoveryServiceConfiguration = ConfigReader.environmentConfiguration().getDiscoveryServiceConfiguration();
        username = ConfigReader.environmentConfiguration().getCredentials().getUser();
        password = ConfigReader.environmentConfiguration().getCredentials().getPassword();
        hosts = discoveryServiceConfiguration.getHost().split(",");
        discoveryRequests = new DiscoveryRequests(hosts[1]);
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
    }

    @Nested
    class GivenHASetUp {
        @Nested
        class whenOneDiscoveryServiceIsNotAvailable {
            @Test
            void serviceStillRegisteredToOtherDiscovery() throws URISyntaxException {
                final int instances = discoveryServiceConfiguration.getInstances();
                assumeTrue(instances > 1);
                String[] hosts = discoveryServiceConfiguration.getHost().split(",");
                shutDownDiscoveryInstance(hosts[0]);
                assertThat(discoveryRequests.isApplicationRegistered("DISCOVERABLECLIENT"), is(true));
            }

            void shutDownDiscoveryInstance(String host) throws URISyntaxException {
                //@formatter:off
                System.out.println(HttpRequestUtils.getUriFromDiscovery(SHUTDOWN, host));
                given()
                    .contentType(JSON)
                    .auth().basic(username, password)
                    .when()
                    .post(HttpRequestUtils.getUriFromDiscovery(SHUTDOWN, host))
                    .then()
                    .statusCode(is(SC_OK))
                    .extract().body().asString();
                //@formatter:on
            }
        }

        @Nested
        class whenOneGatewayIsNotAvailable {
            @Test
            void routeToInstanceThroughAliveGateway() throws IOException {
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
                    .post(HttpRequestUtils.getUriFromGateway(SHUTDOWN, gatewayServiceConfiguration.getPort(), host, Collections.emptyList()))
                    .then()
                    .statusCode(is(SC_OK))
                    .extract().body().asString();
                //@formatter:on
            }
        }
    }

}
