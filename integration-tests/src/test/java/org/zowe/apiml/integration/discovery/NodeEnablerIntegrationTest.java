/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.discovery;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.categories.*;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

/**
 * Test that Node.js enabler is properly integrated with the API ML (Discovery, Gateway)
 */
@TestsNotMeantForZowe
@NotForMainframeTest
@GatewayTest
@NotAttlsTest
@NodeEnablerTest
class NodeEnablerIntegrationTest {

    private static final String APP_INFO_HEALTH = "/hwexpress/api/v1/status/";

    private static final URI MEDIATION_CLIENT_URI = HttpRequestUtils.getUriFromGateway(APP_INFO_HEALTH);

    @BeforeAll
    public static void beforeClass() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Nested
    class WhenServiceIsRegisteredInDiscovery {
        @Test
        void gatewayRouteReturnsUpStatus() {

            await()
                .atMost(2, MINUTES)
                .pollInterval(1, SECONDS)
                .untilAsserted(() ->
                    given()
                        .when()
                        .get(MEDIATION_CLIENT_URI)
                        .then()
                        .statusCode(SC_OK)
                        .body("status", is("UP")));

        }
    }
}
