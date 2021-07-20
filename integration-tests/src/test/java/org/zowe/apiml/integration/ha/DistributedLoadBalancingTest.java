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
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.categories.LbHaTest;
import org.zowe.apiml.util.requests.Apps;
import org.zowe.apiml.util.requests.ha.HADiscoveryRequests;
import org.zowe.apiml.util.requests.ha.HAGatewayRequests;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.zowe.apiml.util.SecurityUtils.*;

@LbHaTest
public class DistributedLoadBalancingTest {

    private final HAGatewayRequests haGatewayRequests = new HAGatewayRequests();
    private final HADiscoveryRequests haDiscoveryRequests = new HADiscoveryRequests();

    @BeforeEach
    void setUp() {
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
    }

    @Test
    void loadBalancerHAtest() {

        assumeTrue(haGatewayRequests.existing() > 1);
        assertThat(haDiscoveryRequests.getAmountOfRegisteredInstancesForService(0, Apps.DISCOVERABLE_CLIENT), is(2));

        String jwt = gatewayToken();

        String header = given()
            .cookie(COOKIE_NAME, jwt)
            .get("https://gateway-service:10010/api/v1/discoverableclient/greeting")
            .header("X-InstanceId");

        assertThat(header, is(nullValue()));

    }
}
