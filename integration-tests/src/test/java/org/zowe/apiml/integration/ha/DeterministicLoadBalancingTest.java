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
import org.zowe.apiml.util.categories.DeterministicLbHaTest;
import org.zowe.apiml.util.requests.Apps;
import org.zowe.apiml.util.requests.ha.HADiscoveryRequests;
import org.zowe.apiml.util.requests.ha.HAGatewayRequests;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.zowe.apiml.util.SecurityUtils.COOKIE_NAME;
import static org.zowe.apiml.util.SecurityUtils.gatewayToken;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;
import static org.zowe.apiml.util.requests.Endpoints.DISCOVERABLE_GREET;

@DeterministicLbHaTest
class DeterministicLoadBalancingTest {

    private final HAGatewayRequests haGatewayRequests = new HAGatewayRequests();
    private final HADiscoveryRequests haDiscoveryRequests = new HADiscoveryRequests();
    private static final String X_INSTANCEID = "X-InstanceId";

    @BeforeEach
    void setUp() {
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
    }

    @Nested
    class GivenDeterministicRoutingViaHeader {

        @Test
        void whenRoutedInstanceExists_thenReturn200() {
            assumeTrue(haGatewayRequests.existing() > 1);
            assertThat(haDiscoveryRequests.getAmountOfRegisteredInstancesForService(0, Apps.DISCOVERABLE_CLIENT), is(2));

            String jwt = gatewayToken();

            String routedInstanceId = given()
                .cookie(COOKIE_NAME, jwt)
            .when()
                .header(X_INSTANCEID, "discoverable-client:discoverableclient:10012")
                .get("https://gateway-service:10010" + DISCOVERABLE_GREET)
            .then()
                .statusCode(is(200))
                .extract()
                .header(X_INSTANCEID);

            assertThat(routedInstanceId, is(notNullValue()));
            assertEquals("discoverable-client:discoverableclient:10012", routedInstanceId);
        }

        @Test
        void whenRoutedDoesNotInstanceExist_thenReturn404() {
            assumeTrue(haGatewayRequests.existing() > 1);
            assertThat(haDiscoveryRequests.getAmountOfRegisteredInstancesForService(0, Apps.DISCOVERABLE_CLIENT), is(2));

            String jwt = gatewayToken();

            String routedInstanceId = given()
                .cookie(COOKIE_NAME, jwt)
            .when()
                .header(X_INSTANCEID, "wrong-discoverable-client:wrong-discoverable-client:10012")
                .get("https://gateway-service:10010" + DISCOVERABLE_GREET)
            .then()
                .statusCode(is(404))
            .extract()
                .header(X_INSTANCEID);

            assertThat(routedInstanceId, is(notNullValue()));
        }
    }

}
