/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.authentication.pat;

import com.nimbusds.jwt.JWTParser;
import io.restassured.response.Response;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;

import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.util.categories.InfinispanStorageTest;
import org.zowe.apiml.util.config.ItSslConfigFactory;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.http.HttpRequestUtils;


import java.net.URI;

import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.apache.commons.lang3.StringUtils.substring;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.*;
import static org.zowe.apiml.util.SecurityUtils.*;
import static org.zowe.apiml.util.requests.Endpoints.*;

public class PATWithAllSchemesTest {

    private static URI URL;
    private static URI URL_SAF;
    private static URI URL_PASS;

    static Stream<Arguments> headers() {
        return Stream.of(Arguments.of("header",ApimlConstants.PAT_HEADER_NAME),
            Arguments.of("header", HttpHeaders.AUTHORIZATION.toLowerCase()),
            Arguments.of("cookie", ApimlConstants.PAT_COOKIE_AUTH_NAME),
            Arguments.of("cookie", COOKIE_NAME));
    }

    String updateHeaders(String headerType,String headerValue, String pat) {

        if ((headerType.equals("header")) && (headerValue.equals("authorization"))) {
            return ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + pat;
        }
        return pat;
    }


    @BeforeAll
   static void init() throws Exception {
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());
        URL = HttpRequestUtils.getUriFromGateway(ZOWE_JWT_REQUEST);
        URL_SAF = HttpRequestUtils.getUriFromGateway(SAF_IDT_REQUEST);
        URL_PASS = HttpRequestUtils.getUriFromGateway(REQUEST_INFO_ENDPOINT);
    }

    @Nested
    class GivenPATWithZowejwtscheme {

        @InfinispanStorageTest
        @ParameterizedTest
        @MethodSource("org.zowe.apiml.integration.authentication.pat.PATWithAllSchemesTest#headers()")
        void requestWithPATZoweZwt(String headerType,String headerValue) throws ParseException {
            Set<String> scopes = new HashSet<>();
            scopes.add("zowejwt");
            String pat = personalAccessToken(scopes);

            String updatedPAT = updateHeaders( headerType, headerValue,  pat);

            if (headerType.equals("header"))
            {
                verifyZoweZwtHeaders(given()
                    .config(SslContext.tlsWithoutCert)
                    .header(headerValue, updatedPAT)
                    .when()
                    .get(URL),pat);
            }
            else {
                verifyZoweZwtHeaders(given()
                    .config(SslContext.tlsWithoutCert)
                    .cookie(headerValue, pat)
                    .when()
                    .get(URL),pat);
            }
        }
        void verifyZoweZwtHeaders(Response response,String pat) throws ParseException {

            assertEquals(response.getStatusCode(), HttpStatus.SC_OK);
            String jwt = substring(response.getBody().path("headers.authorization").toString(),7);

            assertNotEquals(response.getBody().path("headers.authorization"),pat);
            assertEquals(JWTParser.parse(jwt).getJWTClaimsSet().toJSONObject().get("iss"),"APIML");
            assertThat(response.getBody().path("headers.cookie"), containsString(COOKIE_NAME));
        }
    }

    @Nested
    class GivenPATWithPassTicketscheme {


        @InfinispanStorageTest
        @ParameterizedTest
        @MethodSource("org.zowe.apiml.integration.authentication.pat.PATWithAllSchemesTest#headers()")
        void requestWithPATwithPassTicket(String headerType,String headerValue) {
            Set<String> scopes = new HashSet<>();
            scopes.add("dcpassticket");
            String pat = personalAccessToken(scopes);

            String updatedPAT = updateHeaders( headerType, headerValue,  pat);

            if (headerType.equals("header")) {
                verifyPassTicketHeaders(given()
                    .config(SslContext.tlsWithoutCert)
                    .header(headerValue, updatedPAT)
                    .when()
                    .get(URL_PASS));
            }
            else {
                verifyPassTicketHeaders(given()
                    .config(SslContext.tlsWithoutCert)
                    .cookie(headerValue, pat)
                    .when()
                    .get(URL_PASS));
            }
        }

        void verifyPassTicketHeaders(Response response) {
            assertEquals(response.getStatusCode(), HttpStatus.SC_OK);
            assertThat(response.getBody().path("headers.authorization"), startsWith("Basic "));
            assertThat(response.getBody().path("cookies"), not(hasKey(COOKIE_NAME)));
        }
    }

    @Nested
    class GivenPATWithSAFIDTscheme {

        @InfinispanStorageTest
        @ParameterizedTest
        @MethodSource("org.zowe.apiml.integration.authentication.pat.PATWithAllSchemesTest#headers()")
        void requestWithPATWithSafidt(String headerType,String headerValue) {
            Set<String> scopes = new HashSet<>();
            scopes.add("dcsafidt");
            String pat = personalAccessToken(scopes);

            String updatedPAT = updateHeaders( headerType, headerValue,  pat);

            if (headerType.equals("header")) {
                verifySafIDTHeaders(given()
                    .config(SslContext.tlsWithoutCert)
                    .header(headerValue, updatedPAT)
                    .when()
                    .get(URL_SAF));
            }
            else {
                verifySafIDTHeaders(given()
                    .config(SslContext.tlsWithoutCert)
                    .cookie(headerValue, pat)
                    .when()
                    .get(URL_SAF));
            }
        }
        void verifySafIDTHeaders(Response response) {
            assertEquals(response.getStatusCode(), HttpStatus.SC_OK);
            assertThat(response.getBody().path("headers"), hasKey("x-saf-token"));
        }
    }
}
