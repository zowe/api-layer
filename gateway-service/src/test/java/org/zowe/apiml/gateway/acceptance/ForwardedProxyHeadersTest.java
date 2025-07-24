/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.acceptance;

import com.sun.net.httpserver.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.gateway.MockService;
import org.zowe.apiml.gateway.acceptance.common.MicroservicesAcceptanceTest;
import org.zowe.apiml.gateway.acceptance.common.AcceptanceTestWithMockServices;

import static io.restassured.RestAssured.given;
import static org.apache.hc.core5.http.HttpStatus.SC_PERMANENT_REDIRECT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@MicroservicesAcceptanceTest
@ActiveProfiles("ForwardedProxyHeadersTest")
@TestPropertySource(properties = {
    "apiml.service.corsEnabled=false",
    "spring.cloud.gateway.server.webflux.x-forwarded.for-append=false",
    "spring.cloud.gateway.server.webflux.x-forwarded.prefix-enabled=true",
    "spring.cloud.gateway.server.webflux.x-forwarded.prefix-append=true",
    "spring.cloud.gateway.server.webflux.trusted-proxies=.*"
})
class ForwardedProxyHeadersTest extends AcceptanceTestWithMockServices {

    @BeforeEach
    void setUp() {
        var responseHeaders = new Headers();
        responseHeaders.add("Location", basePath + "/serviceid1/test2");
        mockService("serviceid1").scope(MockService.Scope.CLASS)
            .addEndpoint("/serviceid1/test")
                .assertion(he -> assertNotNull(he.getRequestHeaders().getFirst("X-forwarded-prefix")))
                .assertion(he -> assertNotNull(he.getRequestHeaders().getFirst("X-forwarded-for")))
                .assertion(he -> assertFalse(he.getRequestHeaders().getFirst("X-forwarded-for").contains(",")))
                .assertion(he -> assertFalse(he.getRequestHeaders().getFirst("X-forwarded-for").contains("10.0.0.1")))
                .responseCode(SC_PERMANENT_REDIRECT)
                .headers(responseHeaders)
            .and()
            .addEndpoint("/serviceid1/test2")
                .responseCode(SC_OK)
            .and()
            .start();
    }

    @Nested
    class GivenRegisteredServices {

        @Test
        void thenXForwardedHandling() {

            given()
                .log().all()
                .header("X-forwarded-for", "10.0.0.1")
                .header("X-forwarded-prefix", "/test3")
            .when()
                .get(basePath + "/serviceid1/api/v1/test")

            .then()
                .statusCode(is(SC_OK));

        }

    }

}
