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

@AcceptanceTest
@TestPropertySource(properties = {
    "apiml.security.forwardHeader.trusted-proxies=.*"
}
)
class XForwardedHeadersTrustedProxyTest extends XForwardedHeadersProxyTestBase {

    @Test
    void whenNoXForwardHeadersInRequest_ThenXForwardHeadersCreated() {
        given()
            .log().all()
            .when()
            .get(basePath + "/serviceid1/api/v1/xForwardedHeadersCreated")
            .then()
            .statusCode(is(SC_OK));
    }

    @Test
    void whenXForwardHeadersInRequest_ThenXForwardedHeadersNotModified() {
        given()
            .log().all()
            .header("X-forwarded-For", "1.1.1.1")
            .header("X-forwarded-prefix", "/test")
            .when()
            .get(basePath + "/serviceid1/api/v1/xForwardedHeadersForwarded")
            .then()
            .statusCode(is(SC_OK));
    }

    @Test
    void whenXForwardHeadersInRequestFromGW_ThenXForwardedHeadersNotModified() {

        given()
            .config(apimlCert)
            .log().all()
            .header("x-forwarded-For", "1.1.1.1")
            .header("X-forwarded-Prefix", "/test")
            .when()
            .get(basePath + "/serviceid1/api/v1/xForwardedHeadersForwarded")
            .then()
            .statusCode(is(SC_OK));
    }

    @Test
    void whenXForwardHeadersInRequestWithClientCert_ThenXForwardedHeadersNotModified() {

        given()
            .config(clientCert)
            .log().all()
            .header("x-Forwarded-for", "1.1.1.1")
            .header("X-forwarded-prefix", "/test")
            .header("forwarded", "for=1.1.1.1;prefix=/test")
            .when()
            .get(basePath + "/serviceid1/api/v1/xForwardedHeadersForwarded")
            .then()
            .statusCode(is(SC_OK));

    }
}

