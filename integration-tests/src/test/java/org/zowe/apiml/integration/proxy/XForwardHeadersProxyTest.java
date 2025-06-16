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
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.config.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static org.zowe.apiml.util.requests.Endpoints.*;

@Tag("CloudGatewayProxyTest")
class XForwardHeadersProxyTest {

    private static final String HEADER_X_FORWARD_TO = "X-Forward-To";

    static CloudGatewayConfiguration cgwConf;
    static GatewayServiceConfiguration gwConf;

    static String cgwUrl;
    static String gwUrl;

    @BeforeAll
    static void init() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());

        cgwConf = ConfigReader.environmentConfiguration().getCloudGatewayConfiguration();
        gwConf = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();

        cgwUrl = String.format("%s://%s:%s%s", cgwConf.getScheme(), cgwConf.getHost(), cgwConf.getPort(), REQUEST_INFO_ENDPOINT);
        gwUrl = String.format("%s://%s:%s%s", gwConf.getScheme(), gwConf.getHost(), gwConf.getPort(), REQUEST_INFO_ENDPOINT);
    }

    // The request from cloud gateway to gateway is signed and as such trusted
    @Test
    void throughCGW_throughGW_noXForwardHeadersProvided_newXForwardHeadersCreated() {
        given()
            .config(SslContext.clientCertValid)
            .header(HEADER_X_FORWARD_TO, "apiml1")
        .when()
            .get(cgwUrl)
        .then()
            .statusCode(HttpStatus.SC_OK)
            .body("headers.x-forwarded-proto", is("https,https"))
            .body("headers.x-forwarded-prefix", is("/dcpassticket/api/v1"))
            .body("headers.x-forwarded-port", is(cgwConf.getPort() + "," + gwConf.getInternalPorts()))
            .body("headers.x-forwarded-for", not(isEmptyOrNullString()))
            .body("headers.x-forwarded-host", not(isEmptyOrNullString()));
    }

    @Test
    void fromUntrustedProxy_throughCGW_throughGW_xForwardHeadersProvided_untrustedXForwardHeadersForwarded() {
        given()
            .config(SslContext.clientCertValid)
            .header(HEADER_X_FORWARD_TO, "apiml1")
            .header("x-forwarded-proto", "http")
            .header("x-forwarded-prefix", "/untrusted-proxy")
            .header("x-forwarded-port", "666")
            .header("x-forwarded-for", "6.6.6.6")
            .header("x-forwarded-host", "9.9.9.9")
        .when()
            .get(cgwUrl)
        .then()
            .statusCode(HttpStatus.SC_OK)
            .body("headers.x-forwarded-proto", is("https,https"))
            .body("headers.x-forwarded-prefix", is("/dcpassticket/api/v1"))
            .body("headers.x-forwarded-port", is(cgwConf.getPort() + "," + gwConf.getInternalPorts()))
            .body("headers.x-forwarded-for", not(containsString("6.6.6.6")))
            .body("headers.x-forwarded-host", not(containsString("9.9.9.9")));
    }

    @Test
    void fromUntrustedProxy_throughGW_xForwardHeadersProvided_untrustedXForwardHeadersForwarded() {
        given()
            .config(SslContext.clientCertValid)
            .header("x-forwarded-proto", "http")
            .header("x-forwarded-prefix", "/untrusted-proxy")
            .header("x-forwarded-port", "666")
            .header("x-forwarded-for", "6.6.6.6")
            .header("x-forwarded-host", "9.9.9.9")
        .when()
            .get(gwUrl)
        .then()
            .statusCode(HttpStatus.SC_OK)
            .body("headers.x-forwarded-proto", is("https"))
            .body("headers.x-forwarded-prefix", is("/dcpassticket/api/v1"))
            .body("headers.x-forwarded-port", is(String.valueOf(gwConf.getPort())))
            .body("headers.x-forwarded-for", isEmptyOrNullString())
            .body("headers.x-forwarded-host", not(containsString("9.9.9.9")));
    }

}
