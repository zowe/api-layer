/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.startup;

import org.junit.jupiter.api.*;
import org.zowe.apiml.util.categories.EnvironmentCheck;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.EnvironmentConfiguration;

import java.util.Base64;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.core.Is.is;

@EnvironmentCheck
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CheckEnvironment {

    private String username;
    private String password;
    private String zosmfHost;
    private int zosmfPort;
    private String zosmfAuthEndpoint;
    private String zosmfProtectedEndpoint;
    private String zosmfScheme;

    @BeforeEach
    void setUp() {
        EnvironmentConfiguration config = ConfigReader.environmentConfiguration();
        username = config.getCredentials().getUser();
        password = config.getCredentials().getPassword();
        zosmfHost = config.getZosmfServiceConfiguration().getHost();
        zosmfPort = config.getZosmfServiceConfiguration().getPort();
        zosmfAuthEndpoint = "/zosmf/services/authenticate";
        zosmfProtectedEndpoint = "/zosmf/restfiles/ds?dslevel=sys1.p*";
        zosmfScheme = config.getZosmfServiceConfiguration().getScheme();
    }

    @Test
    @Order(1)
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

    @Test
    @Order(2)
    void checkZosmfIsUpAndJwtTokenFromLoginCanBeUsed() {

        // login with Basic and get JWT
        String basicJWT =
            given().auth().basic(username, password)
                .header("X-CSRF-ZOSMF-HEADER", "")
                .when()
                .post(String.format("%s://%s:%d%s", zosmfScheme, zosmfHost, zosmfPort, zosmfAuthEndpoint))
                .then().statusCode(is(SC_OK))
                .extract().cookie("jwtToken");

        // call zOSMF with it
        given()
            .cookie("jwtToken", basicJWT)
            .header("X-CSRF-ZOSMF-HEADER", "")
            .when()
            .get(String.format("%s://%s:%d%s", zosmfScheme, zosmfHost, zosmfPort, zosmfProtectedEndpoint))
            .then().statusCode(SC_OK);
    }

}
