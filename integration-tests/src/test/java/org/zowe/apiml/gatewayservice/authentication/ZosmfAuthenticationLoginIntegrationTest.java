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
import org.junit.jupiter.api.Test;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.util.categories.MainframeDependentTests;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

@MainframeDependentTests
class ZosmfAuthenticationLoginIntegrationTest extends Login {
    private String scheme;
    private String host;
    private int port;
    private final static String ZOSMF_ENDPOINT = "/zosmf/restfiles/ds?dslevel=sys1.p*";

    //TODO this is not running yet, need to be finalized once the functionality is in place

    @BeforeAll
    static void setupClients()  {
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

    /**
     * This is how z/OSMF behaves. Two logins with basic auth give identical token.
     */
    @Test
    void givenValidCredentialsInBody_whenUserAuthenticatesTwice_thenIdenticalTokenIsProduced() {
        LoginRequest loginRequest = new LoginRequest(getUsername(), getPassword());

        String jwtToken1 = authenticateAndVerify(loginRequest);
        String jwtToken2 = authenticateAndVerify(loginRequest);

        assertThat(jwtToken1, is((jwtToken2)));
    }

    //TODO after some thinking, i think this is enough to prove that zOSMF can work with client cert
    @Test
    void givenValidCertificate_whenRequestToZosmfHappensAfterAuthentication_thenTheRequestSucceeds() throws Exception {

        Cookie cookie = given().config(clientCertValid)
            .post(new URI(LOGIN_ENDPOINT_URL))
            .then()
            .statusCode(is(SC_NO_CONTENT))
            .cookie(COOKIE_NAME, not(isEmptyString()))
            .extract().detailedCookie(COOKIE_NAME);

        assertValidAuthToken(cookie);

        String dsname1 = "SYS1.PARMLIB";
        String dsname2 = "SYS1.PROCLIB";

        given().config(tlsWithoutCert)
            .cookie(cookie)
            .get(String.format("%s://%s:%d%s%s", scheme, host, port, BASE_PATH, ZOSMF_ENDPOINT))
            .then()
            .statusCode(is(SC_OK))
            .body(
                "items.dsname", hasItems(dsname1, dsname2));
    }
}
