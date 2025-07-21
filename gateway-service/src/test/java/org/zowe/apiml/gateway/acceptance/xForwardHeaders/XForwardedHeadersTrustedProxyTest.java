/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.acceptance.xForwardHeaders;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.gateway.MockService;
import org.zowe.apiml.gateway.acceptance.common.AcceptanceTestWithMockServices;
import org.zowe.apiml.gateway.acceptance.common.MicroservicesAcceptanceTest;
import org.zowe.apiml.gateway.filters.proxyheaders.AdditionalRegistrationGatewayRegistry;
import org.zowe.apiml.gateway.filters.proxyheaders.X509AndGwAwareXForwardedHeadersFilter;

import java.util.Collections;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicroservicesAcceptanceTest
@TestPropertySource(properties = {
    "apiml.security.forwardHeader.trustedProxies=${test.trustedProxiesPattern}"
})
@ActiveProfiles("forward-headers-proxy-test")
class XForwardedHeadersTrustedProxyTest extends AcceptanceTestWithMockServices {

    @Autowired
    AdditionalRegistrationGatewayRegistry additionalGatewayRegistry;
    @Autowired
    MutateRemoteAddressFilter mutateRemoteAddressFilter;

    @BeforeEach
    void initMockService() {
        mockService("trusted-proxies")
            .scope(MockService.Scope.CLASS)
            .addEndpoint("/trusted-proxies/xForwardedHeadersCreated")
            .assertion(he -> assertEquals(he.getRequestHeaders().getFirst(X509AndGwAwareXForwardedHeadersFilter.X_FORWARDED_FOR_HEADER), clientAddress))
            .assertion(he -> assertNotNull(he.getRequestHeaders().getFirst(X509AndGwAwareXForwardedHeadersFilter.X_FORWARDED_HOST_HEADER)))
            .responseCode(SC_OK)
        .and()
            .addEndpoint("/trusted-proxies/xForwardedHeadersForwarded")
            .assertion(he -> assertTrue(he.getRequestHeaders().getFirst(X509AndGwAwareXForwardedHeadersFilter.X_FORWARDED_PREFIX_HEADER).contains("/test")))
            .assertion(he -> assertTrue(he.getRequestHeaders().getFirst(X509AndGwAwareXForwardedHeadersFilter.X_FORWARDED_FOR_HEADER).contains(proxyAddress)))
            .responseCode(SC_OK)
        .and()
            .addEndpoint("/trusted-proxies/xForwardedHeadersForwardedFromAdditionalGateway")
            .assertion(he -> assertTrue(he.getRequestHeaders().getFirst(X509AndGwAwareXForwardedHeadersFilter.X_FORWARDED_PREFIX_HEADER).contains("/test")))
            .assertion(he -> assertTrue(he.getRequestHeaders().getFirst(X509AndGwAwareXForwardedHeadersFilter.X_FORWARDED_FOR_HEADER).contains(additionalGatewayAddress)))
            .responseCode(SC_OK)
        .and()
            .start();
    }


    @Test
    void whenNoXForwardHeadersInRequest_thenXForwardHeadersCreated() {
        mutateRemoteAddressFilter.proxyAddressReference.set(clientAddress);
        given()
            .log().all()
        .when()
            .get(basePath + "/trusted-proxies/api/v1/xForwardedHeadersCreated")
        .then()
            .statusCode(is(SC_OK));
        mutateRemoteAddressFilter.reset();
    }

    @Test
    void whenXForwardHeadersInRequest_thenXForwardedHeadersForwarded() {
        given()
            .log().all()
            .header("X-forwarded-For", "1.1.1.1")
            .header("X-forwarded-prefix", "/test")
        .when()
            .get(basePath + "/trusted-proxies/api/v1/xForwardedHeadersForwarded")
        .then()
            .statusCode(is(SC_OK));
    }

    @Test
    void whenXForwardHeadersInRequest_fromAdditionalGateway_thenXForwardedHeadersForwarded() {
        mutateRemoteAddressFilter.proxyAddressReference.set(additionalGatewayAddress);
        additionalGatewayRegistry.getAdditionalGatewayIpAddressesReference()
            .set(Collections.singleton(additionalGatewayAddress));

        given()
            .log().all()
            .header("X-forwarded-For", "1.1.1.1")
            .header("X-forwarded-prefix", "/test")
        .when()
            .get(basePath + "/trusted-proxies/api/v1/xForwardedHeadersForwardedFromAdditionalGateway")
        .then()
            .statusCode(is(SC_OK));

        mutateRemoteAddressFilter.reset();
        additionalGatewayRegistry.getAdditionalGatewayIpAddressesReference().set(Collections.emptySet());
    }

    @Test
    void whenXForwardHeadersInRequestFromGW_thenXForwardedHeadersForwarded() {
        given()
            .config(apimlCert)
            .log().all()
            .header("x-forwarded-For", "1.1.1.1")
            .header("X-forwarded-Prefix", "/test")
        .when()
            .get(basePath + "/trusted-proxies/api/v1/xForwardedHeadersForwarded")
        .then()
            .statusCode(is(SC_OK));
    }

    @Test
    void whenXForwardHeadersInRequestWithClientCert_thenXForwardedHeadersForwarded() {
        given()
            .config(clientCert)
            .log().all()
            .header("x-Forwarded-for", "1.1.1.1")
            .header("X-forwarded-prefix", "/test")
        .when()
            .get(basePath + "/trusted-proxies/api/v1/xForwardedHeadersForwarded")
        .then()
            .statusCode(is(SC_OK));
    }
}

