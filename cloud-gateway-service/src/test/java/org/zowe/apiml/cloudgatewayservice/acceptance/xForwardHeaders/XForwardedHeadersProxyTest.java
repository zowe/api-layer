/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.acceptance.xForwardHeaders;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.AcceptanceTest;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.AcceptanceTestWithMockServices;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.MockService;
import org.zowe.apiml.cloudgatewayservice.filters.X509awareXForwardedHeadersFilter;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;

@AcceptanceTest
@TestPropertySource(properties = {
    "apiml.security.forwardHeader.trustedProxies="
})
@ActiveProfiles("forward-headers-proxy-test")
class XForwardedHeadersProxyTest extends AcceptanceTestWithMockServices {

    @BeforeEach
    void initMockService() throws IOException {
        mockService("untrusted-proxies")
            .scope(MockService.Scope.CLASS)
            .addEndpoint("/untrusted-proxies/xForwardedHeadersCreated")
            .assertion(he -> assertNull(he.getRequestHeaders().getFirst(X509awareXForwardedHeadersFilter.X_FORWARDED_FOR_HEADER)))
            .assertion(he -> assertNotNull(he.getRequestHeaders().getFirst(X509awareXForwardedHeadersFilter.X_FORWARDED_HOST_HEADER)))
            .responseCode(SC_OK)
        .and()
            .addEndpoint("/untrusted-proxies/xForwardedHeadersForwarded")
            .assertion(he -> assertTrue(he.getRequestHeaders().getFirst(X509awareXForwardedHeadersFilter.X_FORWARDED_PREFIX_HEADER).contains("/test")))
            .assertion(he -> assertTrue(he.getRequestHeaders().getFirst(X509awareXForwardedHeadersFilter.X_FORWARDED_FOR_HEADER).contains(proxyAddress)))
            .responseCode(SC_OK)
        .and()
            .addEndpoint("/untrusted-proxies/noXForwardedHeadersForwarded")
            // All request headers are stripped, and the untrusted proxy is not present in X-forwarded-for
            // Note: X_FORWARDED_PREFIX_HEADER is processed differently than in the zuul gateway
            .assertion(he -> assertNull(he.getRequestHeaders().getFirst(X509awareXForwardedHeadersFilter.X_FORWARDED_PREFIX_HEADER)))
            .assertion(he -> assertNull(he.getRequestHeaders().getFirst(X509awareXForwardedHeadersFilter.X_FORWARDED_FOR_HEADER)))
            .assertion(he -> assertNotNull(he.getRequestHeaders().getFirst(X509awareXForwardedHeadersFilter.X_FORWARDED_HOST_HEADER)))
            .assertion(he -> assertNull(he.getRequestHeaders().getFirst(X509awareXForwardedHeadersFilter.FORWARDED_HEADER)))
            .responseCode(SC_OK)
        .and()
        .start();
    }

    @Test
    void whenNoXForwardHeadersInRequest_thenXForwardHeadersCreated() {
        given()
            .log().all()
        .when()
            .get(basePath + "/untrusted-proxies/api/v1/xForwardedHeadersCreated")
        .then()
            .statusCode(is(SC_OK));
    }

    @Test
    void whenXForwardHeadersInRequest_thenNoXForwardHeadersForwarded() {
        given()
            .log().all()
            .header("x-Forwarded-for", "1.1.1.1")
            .header("X-forwarded-prefix", "/test")
            .header("forwarded", "for=1.1.1.1;prefix=/test")
        .when()
            .get(basePath + "/untrusted-proxies/api/v1/noXForwardedHeadersForwarded")
        .then()
            .statusCode(is(SC_OK));
    }

    @Test
    void whenXForwardHeadersInRequestFromGW_thenXForwardHeadersForwarded() {
        given()
        .config(apimlCert)
            .log().all()
            .header("x-forwarded-For", "1.1.1.1")
            .header("X-forwarded-Prefix", "/test")
        .when()
            .get(basePath + "/untrusted-proxies/api/v1/xForwardedHeadersForwarded")
        .then()
            .statusCode(is(SC_OK));
    }

    @Test
    void whenXForwardHeadersInRequestWithClientCert_thenNoXForwardHeadersForwarded() {
        given()
            .config(clientCert)
            .log().all()
            .header("x-Forwarded-for", "1.1.1.1")
            .header("X-forwarded-prefix", "/test")
            .header("forwarded", "for=1.1.1.1;prefix=/test")
        .when()
            .get(basePath + "/untrusted-proxies/api/v1/noXForwardedHeadersForwarded")
        .then()
            .statusCode(is(SC_OK));
    }
}
