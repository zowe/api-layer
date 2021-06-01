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

    @BeforeEach
    void setUp() {
        gatewayServiceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
    }

    @Nested
    class GivenMultipleGatewayInstances {
        @Nested
        class WhenSendingRequest {
            @Test
            void gatewayInstancesAreUp() throws IOException {
                final int instances = gatewayServiceConfiguration.getInstances();
                assumeTrue(instances == 2);
                RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
                assertThat(gatewayServiceConfiguration.getInternalPorts(), is(not(nullValue())), is(not("")));

                String[] internalPorts = gatewayServiceConfiguration.getInternalPorts().split(",");
                for (String port : internalPorts) {
                    checkInstancesAreUp(port);
                }
            }

            private void checkInstancesAreUp(String port) throws IOException {
                HttpResponse response = HttpRequestUtils.getResponse(HEALTH_ENDPOINT, HttpStatus.SC_OK, Integer.parseInt(port));
                DocumentContext context = JsonPath.parse(EntityUtils.toString(response.getEntity()));
                Integer amountOfActiveGateways = context.read("$.components.gateway.details.gatewayCount");
                assertThat(amountOfActiveGateways, is(2));
            }
        }
    }
}
