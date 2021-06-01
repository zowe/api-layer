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
import io.restassured.path.xml.XmlPath;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.categories.HATest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.io.IOException;
import java.net.URISyntaxException;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;

/**
 * Verify that multiple southbound services can route through multiples Gateway instances
 */
@HATest
public class SouthboundServicesRoutingTest {
    private GatewayServiceConfiguration gatewayServiceConfiguration;

    private final String DISCOVERABLE_GREET = "/api/v1/discoverableclient/greeting";
    private final String EUREKA_APPS = "/eureka/apps";
    private String username;
    private String password;

    @BeforeEach
    void setUp() {
        gatewayServiceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        username = ConfigReader.environmentConfiguration().getCredentials().getUser();
        password = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    }

    @Nested
    class GivenHASetUp {
        @Nested
        class WhenDiscoverableClientSendRequest {
            @Test
            void routeToAllGatewayInstances() throws IOException {
                final int instances = gatewayServiceConfiguration.getInstances();
                assumeTrue(instances == 2);
                String[] internalPorts = gatewayServiceConfiguration.getInternalPorts().split(",");
                for (String port : internalPorts) {
                    HttpRequestUtils.getResponse(DISCOVERABLE_GREET, SC_OK, Integer.parseInt(port));
                }
            }

            @Test
            void routeToSpecificGatewayInstance() throws URISyntaxException {
                final int instances = gatewayServiceConfiguration.getInstances();
                assumeTrue(instances == 2);
                //@formatter:off
                RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
                given()
                    .when()
                    .header("X-Host", getGatewayInstanceId())
                    .get(HttpRequestUtils.getUriFromGateway(DISCOVERABLE_GREET))
                    .then()
                    .statusCode(is(HttpStatus.SC_OK))
                    .extract().body().asString();
                //@formatter:on
            }

            @Test
            void routeToUndefinedGatewayInstance() {
                final int instances = gatewayServiceConfiguration.getInstances();
                assumeTrue(instances == 2);
                //@formatter:off
                RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
                given()
                    .when()
                    .header("X-Host", "")
                    .get(HttpRequestUtils.getUriFromGateway(DISCOVERABLE_GREET))
                    .then()
                    .statusCode(is(HttpStatus.SC_SERVICE_UNAVAILABLE))
                    .extract().body().asString();
                //@formatter:on
            }

            String getGatewayInstanceId() throws URISyntaxException {
                //@formatter:off
                String xml =
                    given()
                        .auth().basic(username, password)
                        .when()
                        .get(HttpRequestUtils.getUriFromDiscovery(EUREKA_APPS))
                        .then()
                        .statusCode(is(HttpStatus.SC_OK))
                        .extract().body().asString();
                //@formatter:on
                String instanceId = XmlPath.from(xml).getString("applications.application.instance.instanceId");
                assertThat(instanceId, is(not("")));
                return instanceId;
            }
        }
    }
}
