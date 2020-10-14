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
import org.junit.jupiter.api.*;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.util.categories.AuthenticationTest;
import org.zowe.apiml.util.categories.MainframeDependentTests;
import org.zowe.apiml.util.config.*;

import java.net.URI;
import java.util.Base64;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

@AuthenticationTest
class ZosmfAuthenticationLoginIntegrationTest extends Login {
    private String scheme;
    private String host;
    private int port;
    private final static String ZOSMF_ENDPOINT = "/zosmf/restfiles/ds?dslevel=sys1.p*";
    private final static String ZOSMF_SERVICE_ID = ConfigReader.environmentConfiguration().getZosmfServiceConfiguration().getServiceId();
    private final static String ZOSMF_BASE_PATH = "/api/" + ZOSMF_SERVICE_ID;

    private String username;
    private String password;
    private String zosmfHost;
    private int zosmfPort;
    private String zosmfAuthEndpoint;
    private String zosmfProtectedEndpoint;
    private String zosmfScheme;

    @BeforeAll
    static void setupClients()  {
        RestAssured.port = PORT;
        RestAssured.useRelaxedHTTPSValidation();

        providers.switchProvider("zosmf");
    }

    @BeforeEach
    void setUpZosmf() {
        GatewayServiceConfiguration serviceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        scheme = serviceConfiguration.getScheme();
        host = serviceConfiguration.getHost();
        port = serviceConfiguration.getPort();

        EnvironmentConfiguration config = ConfigReader.environmentConfiguration();
        username = config.getCredentials().getUser();
        password = config.getCredentials().getPassword();
        zosmfHost = config.getZosmfServiceConfiguration().getHost();
        zosmfPort = config.getZosmfServiceConfiguration().getPort();
        zosmfAuthEndpoint = "/zosmf/services/authenticate";
        zosmfProtectedEndpoint = "/zosmf/restfiles/ds?dslevel=sys1.p*";
        zosmfScheme = config.getZosmfServiceConfiguration().getScheme();
    }

    /**
     * This is how z/OSMF behaves. Two logins with basic auth give identical token.
     * There is no point to replicate this behavior on mock.
     */
    @Test
    @MainframeDependentTests
    void givenValidCredentialsInBody_whenUserAuthenticatesTwice_thenIdenticalTokenIsProduced() {
        LoginRequest loginRequest = new LoginRequest(getUsername(), getPassword());

        String jwtToken1 = authenticateAndVerify(loginRequest);
        String jwtToken2 = authenticateAndVerify(loginRequest);

        assertThat(jwtToken1, is((jwtToken2)));

        logout(jwtToken1);
    }

    @Test
    void givenValidCertificate_whenRequestToZosmfHappensAfterAuthentication_thenTheRequestSucceeds() throws Exception {

        unblockLockedITUser();

        Cookie cookie = given().config(clientCertValid)
            .post(new URI(LOGIN_ENDPOINT_URL))
            .then()
            .statusCode(is(SC_NO_CONTENT))
            .cookie(COOKIE_NAME, not(isEmptyString()))
            .extract().detailedCookie(COOKIE_NAME);
        assertValidAuthToken(cookie, Optional.of("APIMTST"));

        String dsname1 = "SYS1.PARMLIB";
        String dsname2 = "SYS1.PROCLIB";

        given().config(tlsWithoutCert)
            .cookie(cookie)
            .header("X-CSRF-ZOSMF-HEADER", "")
            .get(String.format("%s://%s:%d%s%s", scheme, host, port, ZOSMF_BASE_PATH, ZOSMF_ENDPOINT))
            .then()
            .statusCode(is(SC_OK))
            .body(
                "items.dsname", hasItems(dsname1, dsname2));

        logout(cookie.getValue());
    }

    void unblockLockedITUser() {
        // login with Basic and get LTPA
        String ltpa2 =
            given()
                .auth().basic(username, password)
                .header("authorization", Base64.getEncoder().encodeToString((username + ":" + password).getBytes()))
                .header("X-CSRF-ZOSMF-HEADER", "")
                .when()
                .post(String.format("%s://%s:%d%s", zosmfScheme, zosmfHost, zosmfPort, zosmfAuthEndpoint))
                .then().statusCode(is(SC_OK))
                .extract().cookie("LtpaToken2");

        // Logout LTPA
        given()
            .header("X-CSRF-ZOSMF-HEADER", "")
            .cookie("LtpaToken2", ltpa2)
            .when()
            .delete(String.format("%s://%s:%d%s", zosmfScheme, zosmfHost, zosmfPort, zosmfAuthEndpoint))
            .then()
            .statusCode(is(SC_NO_CONTENT));
    }
}
