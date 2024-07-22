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
import org.zowe.apiml.util.categories.StickySessionLbHaTest;
import org.zowe.apiml.util.requests.Apps;
import org.zowe.apiml.util.requests.ha.HADiscoveryRequests;
import org.zowe.apiml.util.requests.ha.HAGatewayRequests;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.zowe.apiml.util.SecurityUtils.*;
import static org.zowe.apiml.util.requests.Endpoints.DISCOVERABLE_GREET;

@StickySessionLbHaTest
public class StickySessionLoadBalancingTest {

    private final HAGatewayRequests haGatewayRequests = new HAGatewayRequests();
    private final HADiscoveryRequests haDiscoveryRequests = new HADiscoveryRequests();
    private static final Map<String, String> env = new HashMap<>();
    private final String HOST_HEADER = "host";

    @BeforeEach
    void setUp() {
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
    }

    @Nested
    class GivenAuthenticationLbTypeConfig {

        @Test
        void shouldLoadBalanceSameInstance() {
            assumeTrue(env.get("ZWE_configs_apiml_service_customMetadata_apiml_lb_type").equals("authentication"), "Skipping test: condition not met");

            assumeTrue(haGatewayRequests.existing() > 1);
            assertThat(haDiscoveryRequests.getAmountOfRegisteredInstancesForService(0, Apps.DISCOVERABLE_CLIENT), is(2));

            String jwt = gatewayToken();

            String routedInstanceId = given()
                .cookie(COOKIE_NAME, jwt)
                .get("https://gateway-service:10010" + DISCOVERABLE_GREET)
                .header(HOST_HEADER);

            assertThat(routedInstanceId, is(notNullValue()));

            // Try first on local instance
            String[] results1 = new String[10];
            for (int i = 0; i < 10; i++) {
                String routedInstanceIdOnOtherGateway = given()
                    .cookie(COOKIE_NAME, jwt)
                    .get("https://gateway-service:10010" + DISCOVERABLE_GREET)
                    .header(HOST_HEADER);
                results1[i] = routedInstanceId.equals(routedInstanceIdOnOtherGateway) ? "match" : "nomatch";
            }

            String resultLog1 = String.join(",", results1);
            assertThat("Result of testing against same gateway instance", resultLog1, containsString("match,match,match,match,match,match,match,match,match,match"));


            // Try second on the other instance
            String[] results2 = new String[10];
            for (int i = 0; i < 10; i++) {
                String routedInstanceIdOnOtherGateway = given()
                    .cookie(COOKIE_NAME, jwt)
                    .get("https://gateway-service-2:10010" + DISCOVERABLE_GREET)
                    .header(HOST_HEADER);
                results2[i] = routedInstanceId.equals(routedInstanceIdOnOtherGateway) ? "match" : "nomatch";
            }

            String resultLog2 = String.join(",", results2);
            assertThat("Result of testing against another gateway instance", resultLog2, containsString("match,match,match,match,match,match,match,match,match,match"));
        }
    }
}
