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

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.matchesPattern;
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

    /**
     * Matches one dotted-quad IPv4 address, used to assert x-forwarded-for structure (a
     * comma-separated hop count) without pinning down the exact addresses. Under host-based test
     * execution the addresses a container sees for the client->CGW hop and the CGW->GW hop are
     * Docker's own NAT/bridge addresses (not the test host's identity, and not resolvable via the
     * CGW hostname, which is aliased to 127.0.0.1 on the host) - those differ by platform and
     * Docker network layout, so this only verifies the gateway is genuinely recording one hop per
     * proxy instead of leaking/trusting a client-supplied value.
     */
    private static final String IPV4 = "\\d{1,3}(?:\\.\\d{1,3}){3}";

    @BeforeAll
    static void init() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());

        cgwConf = ConfigReader.environmentConfiguration().getCentralGatewayServiceConfiguration();
        dgwConf = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();

        cgwUrl = String.format("%s://%s:%s%s", cgwConf.getScheme(), cgwConf.getHost(), cgwConf.getPortForHost(cgwConf.getHost()), REQUEST_INFO_ENDPOINT);
        dgwUrl = String.format("%s://%s:%s%s", dgwConf.getScheme(), dgwConf.getHost(), dgwConf.getPortForHost(dgwConf.getHost()), REQUEST_INFO_ENDPOINT);

        jwt = gatewayToken();
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
            .header(HEADER_X_FORWARD_TO, FORWARD_TO_GATEWAY).log().all()
        .when()
            .get(cgwUrl)
        .then()
            .log().ifValidationFails()
            .statusCode(HttpStatus.SC_OK)
            .body("headers.x-forwarded-proto", is("https,https"))
            .body("headers.x-forwarded-prefix", emptyOrNullString())
            .body("headers.x-forwarded-port", is(cgwConf.getPortForHost(cgwConf.getHost()) + "," + dgwConf.getInternalPorts()))
            .body("headers.x-forwarded-for", matchesPattern(IPV4 + "," + IPV4))
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
        .log().all()
        .when()
            .get(cgwUrl)
        .then()
        .log().ifValidationFails()
            .statusCode(HttpStatus.SC_OK)
            .body("headers.x-forwarded-proto", is("https,https"))
            .body("headers.x-forwarded-prefix", emptyOrNullString())
            .body("headers.x-forwarded-port", is(cgwConf.getPortForHost(cgwConf.getHost()) + "," + dgwConf.getInternalPorts()))
            .body("headers.x-forwarded-for", not(containsString("6.6.6.6")))
            .body("headers.x-forwarded-for", matchesPattern(IPV4))
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
            .body("headers.x-forwarded-port", is(String.valueOf(dgwConf.getPortForHost(dgwConf.getHost()))))
            .body("headers.x-forwarded-for", emptyOrNullString())
            .body("headers.x-forwarded-host", not(containsString("9.9.9.9")))
            .body("headers.x-forwarded-host", containsString(dgwConf.getHost()));
    }
}
