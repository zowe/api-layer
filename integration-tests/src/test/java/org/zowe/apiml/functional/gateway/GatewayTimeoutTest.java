/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.functional.gateway;

import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.http.message.BasicNameValuePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;

import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.zowe.apiml.util.http.HttpRequestUtils.getUriFromGateway;
import static org.zowe.apiml.util.requests.Endpoints.DISCOVERABLE_GREET;

@Slf4j
@DiscoverableClientDependentTest
@Disabled("Test fails as timeout happens after far more time")
class GatewayTimeoutTest implements TestWithStartedInstances {
    private static final int SECOND = 1000;
    private static final int DEFAULT_TIMEOUT = 30 * SECOND;

    @BeforeEach
    void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Nested
    class GivenRequestTakesTooLong {
        @Nested
        class WhenCallingThroughGateway {
            @Test
            void returnGatewayTimeout() {
                assertTimeout(Duration.ofMillis(DEFAULT_TIMEOUT * 6 + (2 * SECOND)), () -> {
                    given()
                    .when()
                        .get(getUriFromGateway(DISCOVERABLE_GREET, new BasicNameValuePair("delayMs", String.valueOf(DEFAULT_TIMEOUT + SECOND))))
                        .then()
                        .statusCode(HttpStatus.SC_GATEWAY_TIMEOUT);
                });
            }
        }
    }
}
