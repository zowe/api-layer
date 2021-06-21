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
import io.restassured.path.xml.element.Node;
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
import java.util.Collections;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;

/**
 * Verify that a southbound service can route through multiples Gateway instances
 */
@HATest
public class SouthboundServicesRoutingTest {
    private GatewayServiceConfiguration gatewayServiceConfiguration;

    private final String DISCOVERABLE_GREET = "/api/v1/discoverableclient/greeting";
    private final String EUREKA_APPS = "/eureka/apps";

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
        class WhenDiscoverableClientSendRequest {
            @Test
            void routeToAllGatewayInstances() throws IOException {
                final int instances = gatewayServiceConfiguration.getInstances();
                assumeTrue(instances > 1);
                String[] internalPorts = gatewayServiceConfiguration.getInternalPorts().split(",");
                String[] hosts = gatewayServiceConfiguration.getHost().split(",");
                int port = gatewayServiceConfiguration.getPort();
                assumeTrue(internalPorts.length == instances);
                for (String host : hosts) {
                    HttpRequestUtils.getResponse(DISCOVERABLE_GREET, SC_OK, port, host);
                }
            }

            @Test
            void routeToSpecificInstance() throws URISyntaxException {
                final int instances = gatewayServiceConfiguration.getInstances();
                assumeTrue(instances > 1);
                String host = gatewayServiceConfiguration.getHost().split(",")[0];
                //@formatter:off
                extractHostAndPortMetadata();
                String discoverableClientInstanceId = discoverableClientHost + ":" + "discoverableclient" + ":" + discoverableClientPort;
                given()
                    .when()
                    .header("X-InstanceId", discoverableClientInstanceId)
                    .get(HttpRequestUtils.getUriFromGateway(DISCOVERABLE_GREET, gatewayServiceConfiguration.getPort(), host, Collections.emptyList()))
                    .then()
                    .statusCode(is(HttpStatus.SC_OK))
                    .extract().body().asString();
                //@formatter:on
            }

            @Test
            void routeToUndefinedGatewayInstance() {
                final int instances = gatewayServiceConfiguration.getInstances();
                assumeTrue(instances > 1);
                String host = gatewayServiceConfiguration.getHost().split(",")[0];
                //@formatter:off
                RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
                given()
                    .when()
                    .header("X-InstanceId", "wrongService")
                    .get(HttpRequestUtils.getUriFromGateway(DISCOVERABLE_GREET, gatewayServiceConfiguration.getPort(), host, Collections.emptyList()))
                    .then()
                    .statusCode(is(HttpStatus.SC_SERVICE_UNAVAILABLE))
                    .extract().body().asString();
                //@formatter:on
            }

            void extractHostAndPortMetadata() throws URISyntaxException {
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
                Node discoverableClientNode = XmlPath.from(xml).get("applications.application.find {it.name == 'DISCOVERABLECLIENT'}");
                Node instanceNode = discoverableClientNode.children().get("instance");
                discoverableClientHost = instanceNode.children().get("hostName").toString();
                discoverableClientPort = instanceNode.children().get("securePort").toString();
            }
        }
    }
}
