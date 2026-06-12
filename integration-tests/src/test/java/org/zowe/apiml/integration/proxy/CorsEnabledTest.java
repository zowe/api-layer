/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.proxy;

import io.restassured.http.Header;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.http.HttpRequestUtils;
import org.zowe.apiml.util.requests.Endpoints;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;

@DiscoverableClientDependentTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CorsEnabledTest implements TestWithStartedInstances {

    // // Need to login to get a token and try credentials config.
    // private final GatewayServiceConfiguration gatewayServiceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();

    // private String gatewayScheme;
    // private String gatewayHost;
    // private int gatewayPort;

    @BeforeAll
    void init() {
        // gatewayScheme = gatewayServiceConfiguration.getScheme();
        // gatewayHost = gatewayServiceConfiguration.getHost();
        // gatewayPort = gatewayServiceConfiguration.getPort();
    }

    @Nested
    class WhenCorsIsEnabled {

        @Test
        void givenServiceHasCorsConfiguration_whenPreflightRequestArrives_thenCorsHeadersAreSet() {
            given()
                .header("Origin", "https://foo.bar.org")
            .when()
                .get(HttpRequestUtils.getUriFromGateway(Endpoints.STATIC_CLIENT_1_REQUEST))
            .then()
                .log().all()
                .statusCode(SC_OK)
                .header("Access-Control-Allow-Origin", "https://foo.bar.org");
        }

        @Test
        void givenServiceHasCorsConfiguration_whenSimpleRequestArrives_thenCorsHeadersAreSet() {
            given()
                .header("Origin", "https://foo.bar.org")
            .when()
                .get(HttpRequestUtils.getUriFromGateway(Endpoints.STATIC_CLIENT_1_REQUEST))
            .then()
                .log().all()
                .statusCode(SC_OK)
                .header("Access-Control-Allow-Origin", "https://foo.bar.org");
        }

        @Test
        void givenServiceDoesNotHaveCorsConfiguration_whenPreflightRequestArrives_thenCorsHeadersAreNotSet() {
            given()
                .header("Origin", "https://foo.bar.org")
            .when()
                .options(HttpRequestUtils.getUriFromGateway(Endpoints.STATIC_CLIENT_2_REQUEST))
            .then()
                .log().all()
                .statusCode(SC_OK)
                .header("Access-Control-Allow-Origin", "https://foo.bar.org");
        }

        @Test // TODO check names
        void givenServiceHasIncompleteCorsConfiguration_whenPreflightRequestArrives_thenCorsHeadersAreNotSet() {

        }

    }

    @Nested
    class WhenCorsIsDisabled {

        // preflight (should not be forwarded to service)
        @Test
        void thenForwardToServiceAsIs() {
            given()
                .header(new Header("Origin", ""))
            .when()
                .options("/gateway/version")
            .then()
                .statusCode(SC_OK)
                .header("Access-Control-Allow-Origin", "");
        }

        // simple request (should be forwarded to service)
        @Test
        void simpleRequest() {
            given()
                .header(new Header("Origin", ""))
            .when()
                .get("/gateway/version")
            .then()
                .statusCode(SC_OK)
                .header("Access-Control-Allow-Origin", "");
        }

    }

}
