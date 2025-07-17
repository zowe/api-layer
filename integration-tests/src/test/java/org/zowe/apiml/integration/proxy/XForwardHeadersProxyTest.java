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
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.util.config.CentralGatewayServiceConfiguration;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;
import org.zowe.apiml.util.config.ItSslConfigFactory;
import org.zowe.apiml.util.config.SslContext;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.zowe.apiml.util.SecurityUtils.COOKIE_NAME;
import static org.zowe.apiml.util.SecurityUtils.gatewayToken;
import static org.zowe.apiml.util.requests.Endpoints.REQUEST_INFO_ENDPOINT;

@Tag("GatewayCentralRegistry")
@Slf4j
class XForwardHeadersProxyTest {

    private static final String HEADER_X_FORWARD_TO = "X-Forward-To";
    private static final String FORWARD_TO_GATEWAY = "domain-apiml";

    static CentralGatewayServiceConfiguration cgwConf;
    static GatewayServiceConfiguration dgwConf;

    static String cgwUrl;
    static String dgwUrl;
    static String jwt;

    static String cgwIp;
    static String localIp;

    @BeforeAll
    static void init() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());

        cgwConf = ConfigReader.environmentConfiguration().getCentralGatewayServiceConfiguration();
        dgwConf = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();

        cgwUrl = String.format("%s://%s:%s%s", cgwConf.getScheme(), cgwConf.getHost(), cgwConf.getPort(), REQUEST_INFO_ENDPOINT);
        dgwUrl = String.format("%s://%s:%s%s", dgwConf.getScheme(), dgwConf.getHost(), dgwConf.getPort(), REQUEST_INFO_ENDPOINT);

        jwt = gatewayToken();

        cgwIp = InetAddress.getByName(cgwConf.getHost()).getHostAddress();
        localIp = InetAddress.getLocalHost().getHostAddress();

        log.debug("Central GW hostname and IP Address: {}: {}", cgwConf.getHost(), Arrays.toString(InetAddress.getAllByName(cgwConf.getHost())));
        log.debug("Domain GW hostname and IP Address: {}: {}", dgwConf.getHost(), Arrays.toString(InetAddress.getAllByName(dgwConf.getHost())));
        log.debug("Local IP Address: {}", InetAddress.getLocalHost().getHostAddress());
    }

    private static Stream<Arguments> authenticationRequestSpecifications() {
        return Stream.of(
            Arguments.of(given().config(SslContext.clientCertValid)),
            Arguments.of(given().cookie(COOKIE_NAME, jwt))
        );
    }

    @ParameterizedTest
    @MethodSource("authenticationRequestSpecifications")
    void throughCGW_throughGW_noXForwardHeadersProvided_newXForwardHeadersCreated(RequestSpecification requestSpecs) {
        requestSpecs
            .header(HEADER_X_FORWARD_TO, FORWARD_TO_GATEWAY)
        .when()
            .get(cgwUrl)
        .then()
            .statusCode(HttpStatus.SC_OK)
            .body("headers.x-forwarded-proto", is("https,https"))
            .body("headers.x-forwarded-prefix", emptyOrNullString())
            .body("headers.x-forwarded-port", is(cgwConf.getPort() + "," + dgwConf.getInternalPorts()))
            .body("headers.x-forwarded-for", containsString(cgwIp))
            .body("headers.x-forwarded-for", containsString(localIp))
            .body("headers.x-forwarded-host", containsString(cgwConf.getHost()))
            .body("headers.x-forwarded-host", containsString(dgwConf.getHost()));
    }

    @ParameterizedTest
    @MethodSource("authenticationRequestSpecifications")
    void fromUntrustedProxy_throughCGW_throughGW_xForwardHeadersProvided_untrustedXForwardHeadersNotForwarded(RequestSpecification requestSpecs) {
        requestSpecs
            .header(HEADER_X_FORWARD_TO, FORWARD_TO_GATEWAY)
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
            .body("headers.x-forwarded-prefix", emptyOrNullString())
            .body("headers.x-forwarded-port", is(cgwConf.getPort() + "," + dgwConf.getInternalPorts()))
            .body("headers.x-forwarded-for", not(containsString("6.6.6.6")))
            .body("headers.x-forwarded-for", is(cgwIp))
            .body("headers.x-forwarded-host", not(containsString("9.9.9.9")))
            .body("headers.x-forwarded-host", containsString(cgwConf.getHost()))
            .body("headers.x-forwarded-host", containsString(dgwConf.getHost()));
    }

    @ParameterizedTest
    @MethodSource("authenticationRequestSpecifications")
    void fromUntrustedProxy_throughGW_xForwardHeadersProvided_untrustedXForwardHeadersNotForwarded(RequestSpecification requestSpecs) {
        requestSpecs
            .header("x-forwarded-proto", "http")
            .header("x-forwarded-prefix", "/untrusted-proxy")
            .header("x-forwarded-port", "666")
            .header("x-forwarded-for", "6.6.6.6")
            .header("x-forwarded-host", "9.9.9.9")
        .when()
            .get(dgwUrl)
        .then()
            .statusCode(HttpStatus.SC_OK)
            .body("headers.x-forwarded-proto", is("https"))
            .body("headers.x-forwarded-prefix", emptyOrNullString())
            .body("headers.x-forwarded-port", is(String.valueOf(dgwConf.getPort())))
            .body("headers.x-forwarded-for", emptyOrNullString())
            .body("headers.x-forwarded-host", not(containsString("9.9.9.9")))
            .body("headers.x-forwarded-host", containsString(dgwConf.getHost()));
    }
}
