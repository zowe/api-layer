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

import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.AcceptanceTest;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AcceptanceTest
@TestPropertySource(properties = {
    "apiml.security.forwardHeader.trusted-proxies="
}
)
class XForwardedHeadersProxyTest extends XForwardedHeadersProxyTestBase {

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
    void whenXForwardHeadersInRequest_ThenNoXForwardHeadersForwarded() {
        given()
            .log().all()
            .header("x-Forwarded-for", "1.1.1.1")
            .header("X-forwarded-prefix", "/test")
            .header("forwarded", "for=1.1.1.1;prefix=/test")
            .when()
            .get(basePath + "/serviceid1/api/v1/noXForwardedHeadersForwarded")
            .then()
            .statusCode(is(SC_OK));
    }

    @Test
    void whenXForwardHeadersInRequestFromGW_ThenXForwardHeadersForwarded() {

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
    void whenXForwardHeadersInRequestWithClientCert_ThenNoXForwardHeadersForwarded() {

        given()
            .config(clientCert)
            .log().all()
            .header("x-Forwarded-for", "1.1.1.1")
            .header("X-forwarded-prefix", "/test")
            .header("forwarded", "for=1.1.1.1;prefix=/test")
            .when()
            .get(basePath + "/serviceid1/api/v1/noXForwardedHeadersForwarded")
            .then()
            .statusCode(is(SC_OK));

    }
}
