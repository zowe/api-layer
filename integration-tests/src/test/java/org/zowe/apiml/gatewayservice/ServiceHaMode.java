/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gatewayservice;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.config.RandomPort;
import org.zowe.apiml.util.service.VirtualService;

import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;
import static org.zowe.apiml.gatewayservice.SecurityUtils.getConfiguredSslConfig;

/**
 * Objective is to test Gateway can retry on service that is down.
 *
 * 2 services are registered and after that, one is killed. Service is called through Gateway
 * and responses are inspected. Implementation returns a debug header that describes the retries.
 * The test repeats calls until it sees that request has been retried from mentioned header.
 */
@TestsNotMeantForZowe
class ServiceHaMode {
    private static final int TIMEOUT = 30;

    @BeforeEach
    void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
    }

    @Test
    void givenTwoServices_whenOneServiceGoesDown_verifyThatGatewayRetriesToTheLiveOne() throws Exception {

        try (
            VirtualService service1 = new VirtualService("testHaModeService", (new RandomPort()).getPort());
            VirtualService service2 = new VirtualService("testHaModeService", (new RandomPort()).getPort());
            ) {

            service1.start();
            service2.start().waitForGatewayRegistration(2, TIMEOUT);
            service2.zombie();

            routeAndVerifyRetry(service1.getGatewayUrls(), TIMEOUT);
        }
    }

    private void routeAndVerifyRetry(List<String> gatewayUrls, int timeoutSec) {
        final long time0 = System.currentTimeMillis();

        for (String gatewayUrl : gatewayUrls) {
            while (true) {
                String url = gatewayUrl + "/application/instance";

                try {
                    Response response = given().when()
                        .get(url)
                        .andReturn();
                    if (response.getStatusCode() != HttpStatus.SC_OK) {
                        fail();
                    }
                    StringTokenizer retryList = new StringTokenizer(response.getHeader("RibbonRetryDebug"), "|");
                    assertThat(retryList.countTokens(), is(greaterThan(1)));
                    break;
                }
                catch (RuntimeException | AssertionError e) {
                    if (System.currentTimeMillis() - time0 > timeoutSec * 1000) throw e;
                    await().timeout(1, TimeUnit.SECONDS);
                }
            }
        }
    }
}
