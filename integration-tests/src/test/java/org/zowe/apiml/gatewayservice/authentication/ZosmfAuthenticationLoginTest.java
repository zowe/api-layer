/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gatewayservice.authentication;

import io.restassured.RestAssured;
import io.restassured.http.Cookie;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.util.categories.zOSMFAuthTest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;
import org.zowe.apiml.util.config.SslContext;

import java.net.URI;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

@zOSMFAuthTest
class ZosmfAuthenticationLoginTest extends LoginTest {
    private String scheme;
    private String host;
    private int port;
    private final static String ZOSMF_ENDPOINT = "/zosmf/restfiles/ds?dslevel=sys1.p*";
    private final static String ZOSMF_SERVICE_ID = ConfigReader.environmentConfiguration().getZosmfServiceConfiguration().getServiceId();
    private final static String ZOSMF_BASE_PATH = "/api/" + ZOSMF_SERVICE_ID;


    @BeforeAll
    static void setupClients() {
        RestAssured.port = PORT;
        RestAssured.useRelaxedHTTPSValidation();

    }

    @BeforeEach
    void setUpZosmf() {
        GatewayServiceConfiguration serviceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        scheme = serviceConfiguration.getScheme();
        host = serviceConfiguration.getHost();
        port = serviceConfiguration.getPort();
    }

    @ParameterizedTest
    @MethodSource("loginUrlsSource")
    void givenValidCertificate_whenRequestToZosmfHappensAfterAuthentication_thenTheRequestSucceeds(String loginUrl) throws Exception {
        Cookie cookie = given().config(SslContext.clientCertValid)
            .post(new URI(loginUrl))
            .then()
            .statusCode(is(SC_NO_CONTENT))
            .cookie(COOKIE_NAME, not(isEmptyString()))
            .extract().detailedCookie(COOKIE_NAME);
        assertValidAuthToken(cookie, Optional.of("APIMTST"));

        String dsname1 = "SYS1.PARMLIB";
        String dsname2 = "SYS1.PROCLIB";

        given().config(SslContext.tlsWithoutCert)
            .cookie(cookie)
            .header("X-CSRF-ZOSMF-HEADER", "")
            .get(String.format("%s://%s:%d%s%s", scheme, host, port, ZOSMF_BASE_PATH, ZOSMF_ENDPOINT))
            .then()
            .statusCode(is(SC_OK))
            .body(
                "items.dsname", hasItems(dsname1, dsname2));
    }

    @ParameterizedTest
    @MethodSource("loginUrlsSource")
    void givenClientX509Cert_whenUserAuthenticates_thenTheValidTokenIsProduced(String loginUrl) throws Exception {
        Cookie cookie = given().config(SslContext.clientCertValid)
            .post(new URI(loginUrl))
            .then()
            .statusCode(is(SC_NO_CONTENT))
            .cookie(COOKIE_NAME, not(isEmptyString()))
            .extract().detailedCookie(COOKIE_NAME);

        assertValidAuthToken(cookie, Optional.of("APIMTST"));
    }

    @ParameterizedTest
    @MethodSource("loginUrlsSource")
    void givenValidClientCertAndInvalidBasic_whenAuth_thenCertShouldTakePrecedenceAndTokenIsProduced(String loginUrl) throws Exception {
        Cookie cookie = given().config(SslContext.clientCertValid)
            .auth().basic("Bob", "The Builder")
            .post(new URI(loginUrl))
            .then()
            .statusCode(is(SC_NO_CONTENT))
            .cookie(COOKIE_NAME, not(isEmptyString()))
            .extract().detailedCookie(COOKIE_NAME);

        assertValidAuthToken(cookie, Optional.of("APIMTST"));
    }
}
