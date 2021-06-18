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

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.restassured.RestAssured;
import io.restassured.path.xml.XmlPath;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import org.apache.http.util.EntityUtils;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assumptions.*;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;

/**
 * Verify that both Gateway instances are UP
 */
@HATest
public class GatewayMultipleInstancesTest {
    private GatewayServiceConfiguration gatewayServiceConfiguration;
    private final String HEALTH_ENDPOINT = "/application/health";
    private final String EUREKA_APPS = "/eureka/apps";
    private String username;
    private String password;
    private int instances;

    @BeforeEach
    void setUp() {
        gatewayServiceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        username = ConfigReader.environmentConfiguration().getCredentials().getUser();
        password = ConfigReader.environmentConfiguration().getCredentials().getPassword();
        instances = gatewayServiceConfiguration.getInstances();
    }

    @Nested
    class GivenMultipleGatewayInstances {
        @Nested
        class WhenSendingRequest {
            @Test
            void gatewayInstancesAreUp() throws IOException {
                assumeTrue(instances > 1);
                RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
                assertThat(gatewayServiceConfiguration.getInternalPorts(), is(not(nullValue())), is(not("")));

                checkInstancesAreUp();
            }

            @Test
            void gatewayInstancesAreRegistered() throws URISyntaxException {
                assumeTrue(instances > 1);
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
                String[] internalPorts = gatewayServiceConfiguration.getInternalPorts().split(",");
                String[] hosts = gatewayServiceConfiguration.getHost().split(",");

                for (String host : hosts) {
                    assertThat(instanceId.contains(host + ":" + "gateway" + ":"), is(true));
                }
                for (String port : internalPorts) {
                    assertThat(instanceId.contains(":" + "gateway" + ":" + port), is(true));
                }
            }

            private void checkInstancesAreUp() throws IOException {
                String[] hosts = gatewayServiceConfiguration.getHost().split(",");
                int port = gatewayServiceConfiguration.getPort();
                for (String host : hosts) {
                    HttpResponse response = HttpRequestUtils.getResponse(HEALTH_ENDPOINT, HttpStatus.SC_OK, port, host);
                    DocumentContext context = JsonPath.parse(EntityUtils.toString(response.getEntity()));
                    Integer amountOfActiveGateways = context.read("$.components.gateway.details.gatewayCount");
                    assertThat(amountOfActiveGateways, is(instances));
                }

            }
        }
    }
}
