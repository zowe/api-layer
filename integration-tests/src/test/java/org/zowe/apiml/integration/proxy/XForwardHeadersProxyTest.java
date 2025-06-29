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
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.config.*;

import java.net.InetAddress;
import java.util.Arrays;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static org.zowe.apiml.util.SecurityUtils.COOKIE_NAME;
import static org.zowe.apiml.util.SecurityUtils.gatewayToken;
import static org.zowe.apiml.util.requests.Endpoints.REQUEST_INFO_ENDPOINT;

@Tag("CloudGatewayCentralRegistry")
@Slf4j
class XForwardHeadersProxyTest {

    private static final String HEADER_X_FORWARD_TO = "X-Forward-To";
    private static final String FORWARD_TO_GATEWAY = "domain-apiml";

    static CloudGatewayConfiguration cgwConf;
    static GatewayServiceConfiguration gwConf;

    static String cgwUrl;
    static String gwUrl;
    static String jwt;

    static String cgwIp;
    static String localIp;

    @BeforeAll
    static void init() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());

        cgwConf = ConfigReader.environmentConfiguration().getCloudGatewayConfiguration();
        gwConf = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();

        cgwUrl = String.format("%s://%s:%s%s", cgwConf.getScheme(), cgwConf.getHost(), cgwConf.getPort(), REQUEST_INFO_ENDPOINT);
        gwUrl = String.format("%s://%s:%s%s", gwConf.getScheme(), gwConf.getHost(), gwConf.getPort(), REQUEST_INFO_ENDPOINT);

        jwt = gatewayToken();

        cgwIp = InetAddress.getByName(cgwConf.getHost()).getHostAddress();
        localIp = InetAddress.getLocalHost().getHostAddress();

        log.info("CGW hostname and IP Address: {}: {}", cgwConf.getHost(), Arrays.toString(InetAddress.getAllByName(cgwConf.getHost())));
        log.info("GW hostname and IP Address: {}: {}", gwConf.getHost(), Arrays.toString(InetAddress.getAllByName(gwConf.getHost())));
        log.info("Local IP Address: {}", InetAddress.getLocalHost().getHostAddress());
    }

    @Test
    void throughCGW_throughGW_noXForwardHeadersProvided_newXForwardHeadersCreated() {
        given().log().all()
            .cookie(COOKIE_NAME, jwt)
            .header(HEADER_X_FORWARD_TO, FORWARD_TO_GATEWAY)
        .when()
            .get(cgwUrl)
        .then()
            .log().all()
            .statusCode(HttpStatus.SC_OK)
            .body("headers.x-forwarded-proto", is("https,https"))
            .body("headers.x-forwarded-prefix", is("/dcpassticket/api/v1"))
            .body("headers.x-forwarded-port", is(cgwConf.getPort() + "," + gwConf.getInternalPorts()))
            .body("headers.x-forwarded-for", containsString(cgwIp))
            .body("headers.x-forwarded-for", containsString(localIp))
            .body("headers.x-forwarded-host", containsString(cgwConf.getHost()))
            .body("headers.x-forwarded-host", containsString(gwConf.getHost()));
    }

    @Test
    void fromUntrustedProxy_throughCGW_throughGW_xForwardHeadersProvided_untrustedXForwardHeadersNotForwarded() {
        given().log().all()
            .cookie(COOKIE_NAME, jwt)
            .header(HEADER_X_FORWARD_TO, FORWARD_TO_GATEWAY)
            .header("x-forwarded-proto", "http")
            .header("x-forwarded-prefix", "/untrusted-proxy")
            .header("x-forwarded-port", "666")
            .header("x-forwarded-for", "6.6.6.6")
            .header("x-forwarded-host", "9.9.9.9")
        .when()
            .get(cgwUrl)
        .then()
            .log().all()
            .statusCode(HttpStatus.SC_OK)
            .body("headers.x-forwarded-proto", is("https,https"))
            .body("headers.x-forwarded-prefix", is("/dcpassticket/api/v1"))
            .body("headers.x-forwarded-port", is(cgwConf.getPort() + "," + gwConf.getInternalPorts()))
            .body("headers.x-forwarded-for", not(containsString("6.6.6.6")))
            .body("headers.x-forwarded-for", is(cgwIp))
            .body("headers.x-forwarded-host", not(containsString("9.9.9.9")))
            .body("headers.x-forwarded-host", containsString(cgwConf.getHost()))
            .body("headers.x-forwarded-host", containsString(gwConf.getHost()));
    }

    @Test
    void fromUntrustedProxy_throughGW_xForwardHeadersProvided_untrustedXForwardHeadersNotForwarded() {
        given().log().all()
            .cookie(COOKIE_NAME, jwt)
            .header("x-forwarded-proto", "http")
            .header("x-forwarded-prefix", "/untrusted-proxy")
            .header("x-forwarded-port", "666")
            .header("x-forwarded-for", "6.6.6.6")
            .header("x-forwarded-host", "9.9.9.9")
        .when()
            .get(gwUrl)
        .then()
            .log().all()
            .statusCode(HttpStatus.SC_OK)
            .body("headers.x-forwarded-proto", is("https"))
            .body("headers.x-forwarded-prefix", is("/dcpassticket/api/v1"))
            .body("headers.x-forwarded-port", is(String.valueOf(gwConf.getPort())))
            .body("headers.x-forwarded-for", isEmptyOrNullString())
            .body("headers.x-forwarded-host", not(containsString("9.9.9.9")))
            .body("headers.x-forwarded-host", containsString(gwConf.getHost()));
    }

}
