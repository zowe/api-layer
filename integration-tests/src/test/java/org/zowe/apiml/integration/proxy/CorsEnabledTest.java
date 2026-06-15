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

import io.restassured.RestAssured;
import io.restassured.http.Header;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;
import org.zowe.apiml.util.config.ItSslConfigFactory;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.http.HttpRequestUtils;
import org.zowe.apiml.util.requests.Endpoints;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;

@DiscoverableClientDependentTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CorsEnabledTest implements TestWithStartedInstances {

    private final GatewayServiceConfiguration gatewayServiceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();

    @BeforeAll
    void init() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());
    }

    @Nested
    class WhenCorsIsEnabledInService {

        @ParameterizedTest
        @CsvSource({
            "https://foo.bar.org, 200",
            "https://localhost:10010, 403"
        })
        void test1(String origin, int statusCode) {
            given()
                .log().all()
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Origin")
            .when()
                .options(HttpRequestUtils.getUriFromGateway(Endpoints.STATIC_CLIENT_1_REQUEST))
            .then()
                .log().all()
                .statusCode(statusCode);
        }

        @Test
        void test3() {
            given()
                .header("Origin", "https://foo.bar.org")
            .when()
                .options(HttpRequestUtils.getUriFromGateway(Endpoints.STATIC_CLIENT_1_REQUEST))
            .then()
                .log().all()
                .statusCode(SC_OK)
                .header("Access-Control-Allow-Origin", "https://foo.bar.org");
        }

        @Test // TODO check names
        void test4() {

        }

    }

    @Nested
    class WhenCorsIsDisabledInService {

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
