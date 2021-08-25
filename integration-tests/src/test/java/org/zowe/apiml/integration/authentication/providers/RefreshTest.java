/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.authentication.providers;

import io.restassured.RestAssured;
import io.restassured.http.Cookie;
import org.junit.jupiter.api.*;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.*;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.zowe.apiml.util.SecurityUtils.*;

@GeneralAuthenticationTest
@SAFAuthTest
@zOSMFAuthTest
public class RefreshTest implements TestWithStartedInstances {

    public static final AuthConfigurationProperties authConfigurationProperties = new AuthConfigurationProperties();

    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    public static final URI REFRESH_URL = HttpRequestUtils.getUriFromGateway(authConfigurationProperties.getGatewayRefreshEndpointNewFormat());

    @BeforeAll
    public static void init() throws Exception {
        SslContext.prepareSslAuthentication();
    }
    @BeforeEach
    public void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Nested
    class GivenIllegalAccessModes {
        @Test
        void noClientCertificateGivesForbidden() {

            given().config(SslContext.tlsWithoutCert)
                .when()
                .post(REFRESH_URL)
                .then()
                .statusCode(is(SC_FORBIDDEN));
        }
        @Test
        void clientCertificateWithoutTokenGivesUnauthorized() {
            LoginRequest loginRequest = new LoginRequest(USERNAME, PASSWORD);

            given().config(SslContext.clientCertApiml)
                .when()
                .contentType(JSON)
                .body(loginRequest)
                .post(REFRESH_URL)
                .then()
                .statusCode(is(SC_UNAUTHORIZED));
        }
    }

    @Nested
    class GivenLegalAccessModes {
        @Test
        void whenJwtTokenPostedCanBeRefreshedAndOldCookieInvalidated() {

            String cookie = gatewayToken();

            Cookie refreshedCookie = given().config(SslContext.clientCertApiml)
                .cookie(COOKIE_NAME, cookie)
                .when()
                .post(REFRESH_URL)
                .then()
                .statusCode(is(SC_NO_CONTENT))
                .header("Set-Cookie", containsString("SameSite=Strict"))
                .cookie(COOKIE_NAME, not(isEmptyString()))
                .extract().detailedCookie(COOKIE_NAME);

            assertThat(refreshedCookie.getValue(), is(not(cookie)));
            assertValidAuthToken(refreshedCookie);

            assertIfLogged(refreshedCookie.getValue(), true);
            assertIfLogged(cookie, false);

        }

    }


}
