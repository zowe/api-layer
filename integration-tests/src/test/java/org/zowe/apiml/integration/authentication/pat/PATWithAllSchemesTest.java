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

import io.restassured.response.Response;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.categories.InfinispanStorageTest;
import org.zowe.apiml.util.config.ItSslConfigFactory;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static io.restassured.RestAssured.given;
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

    @BeforeAll
   static void init() throws Exception {
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());
        URL = HttpRequestUtils.getUriFromGateway(ZOWE_JWT_REQUEST);
        URL_SAF = HttpRequestUtils.getUriFromGateway(SAF_IDT_REQUEST);
        URL_PASS = HttpRequestUtils.getUriFromGateway(REQUEST_INFO_ENDPOINT);
    }

    @Nested
    class GivenPATWithZowejwtscheme {
        @Test
        @InfinispanStorageTest
        void requestWithPATinPATHeader() {
            Set<String> scopes = new HashSet<>();
            scopes.add("zowejwt");
            String pat = personalAccessToken(scopes);

            verifyZoweZwtHeaders(given()
                .config(SslContext.tlsWithoutCert)
                .header(ApimlConstants.PAT_HEADER_NAME, pat)
                .when()
                .get(URL),pat);
        }

        @Test
        @InfinispanStorageTest
        void requestWithPATinAuthorizationHeader() {
            Set<String> scopes = new HashSet<>();
            scopes.add("zowejwt");
            String pat = personalAccessToken(scopes);

            verifyZoweZwtHeaders(given()
                .config(SslContext.tlsWithoutCert)
                .header(HttpHeaders.AUTHORIZATION,
                    ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + pat)
                .when()
                .get(URL),pat);
        }

        @Test
        @InfinispanStorageTest
        void requestWithPATinPATCookieHeader() {
            Set<String> scopes = new HashSet<>();
            scopes.add("zowejwt");
            String pat = personalAccessToken(scopes);

            verifyZoweZwtHeaders(given()
                .config(SslContext.tlsWithoutCert)
                .cookie(ApimlConstants.PAT_COOKIE_AUTH_NAME, pat)
                .when()
                .get(URL),pat);
        }

        @Test
        @InfinispanStorageTest
        void requestWithPATinCookieHeader() {
            Set<String> scopes = new HashSet<>();
            scopes.add("zowejwt");
            String pat = personalAccessToken(scopes);

          verifyZoweZwtHeaders(given()
                .config(SslContext.tlsWithoutCert)
                .cookie(SecurityUtils.COOKIE_NAME, pat)
                .when()
                .get(URL),pat);
        }

        void verifyZoweZwtHeaders(Response response,String pat) {
            String basic = "Basic " + Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes());
            assertEquals(response.getStatusCode(), HttpStatus.SC_OK);
            assertNotEquals(response.getBody().path("headers.authorization"),pat);
            assertNotEquals(response.getBody().path("headers.authorization"), basic);
            assertThat(response.getBody().path("headers.cookie"), containsString(COOKIE_NAME));
        }

    }

    @Nested
    class GivenPATWithPassTicketscheme {

        @Test
        @InfinispanStorageTest
        void requestWithPATinPATCookieHeader() {
            Set<String> scopes = new HashSet<>();
            scopes.add("dcpassticket");
            String pat = personalAccessToken(scopes);

            verifyPassTicketHeaders(given()
                .config(SslContext.tlsWithoutCert)
                .cookie(ApimlConstants.PAT_COOKIE_AUTH_NAME, pat)
                .when()
                .get(URL_PASS));
        }

        @Test
        @InfinispanStorageTest
        void requestWithPATinAuthorizationHeader() {
            Set<String> scopes = new HashSet<>();
            scopes.add("dcpassticket");
            String pat = personalAccessToken(scopes);

            verifyPassTicketHeaders(given()
                    .config(SslContext.tlsWithoutCert)
                    .header(HttpHeaders.AUTHORIZATION,
                        ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + pat)
                    .when()
                    .get(URL_PASS));
        }

        @Test
        @InfinispanStorageTest
        void requestWithPATinPATHeader() {
            Set<String> scopes = new HashSet<>();
            scopes.add("dcpassticket");
            String pat = personalAccessToken(scopes);

            verifyPassTicketHeaders(given()
                .config(SslContext.tlsWithoutCert)
                .header(ApimlConstants.PAT_HEADER_NAME, pat)
                .when()
                .get(URL_PASS));
        }

        @Test
        @InfinispanStorageTest
        void requestWithPATinCookieHeader() {
            Set<String> scopes = new HashSet<>();
            scopes.add("dcpassticket");
            String pat = personalAccessToken(scopes);

            verifyPassTicketHeaders(given()
                .config(SslContext.tlsWithoutCert)
                .cookie(SecurityUtils.COOKIE_NAME, pat)
                .when()
                .get(URL_PASS));
        }

        void verifyPassTicketHeaders(Response response) {
            String basic = "Basic " + Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes());
            assertEquals(response.getStatusCode(), HttpStatus.SC_OK);
            assertThat(response.getBody().path("headers.authorization"), startsWith("Basic "));
            assertNotEquals(response.getBody().path("headers.authorization"), basic);
            assertThat(response.getBody().path("cookies"), not(hasKey(COOKIE_NAME)));
        }
    }

    @Nested
    class GivenPATWithSAFIDTscheme {

        @Test
        @InfinispanStorageTest
        void requestWithPATinPATHeader() {
            Set<String> scopes = new HashSet<>();
            scopes.add("dcsafidt");
            String pat = personalAccessToken(scopes);

            verifySafIDTHeaders(given()
                .config(SslContext.tlsWithoutCert)
                .header(ApimlConstants.PAT_HEADER_NAME, pat)
                .when()
                .get(URL_SAF));
        }

        @Test
        @InfinispanStorageTest
        void requestWithPATinAuthorizationHeader() {
            Set<String> scopes = new HashSet<>();
            scopes.add("dcsafidt");
            String pat = personalAccessToken(scopes);

            verifySafIDTHeaders(given()
                    .config(SslContext.tlsWithoutCert)
                    .header(HttpHeaders.AUTHORIZATION,
                        ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + pat)
                    .when()
                    .get(URL_SAF));
        }

        @Test
        @InfinispanStorageTest
        void requestWithPATinPATCookieHeader() {
            Set<String> scopes = new HashSet<>();
            scopes.add("dcsafidt");
            String pat = personalAccessToken(scopes);

            verifySafIDTHeaders(given()
                .config(SslContext.tlsWithoutCert)
                .cookie(ApimlConstants.PAT_COOKIE_AUTH_NAME, pat)
                .when()
                .get(URL_SAF));
        }

        @Test
        @InfinispanStorageTest
        void requestWithPATinCookieHeader() {
            Set<String> scopes = new HashSet<>();
            scopes.add("dcsafidt");
            String pat = personalAccessToken(scopes);

            verifySafIDTHeaders(given()
                .config(SslContext.tlsWithoutCert)
                .cookie(SecurityUtils.COOKIE_NAME, pat)
                .when()
                .get(URL_SAF));
        }

        void verifySafIDTHeaders(Response response) {
            String basic = "Basic " + Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes());
            assertEquals(response.getStatusCode(), HttpStatus.SC_OK);
            assertNotEquals(response.getBody().path("headers.authorization"), basic);
            assertThat(response.getBody().path("headers"), hasKey("x-saf-token"));
        }
    }
}
