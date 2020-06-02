/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.discoverableclient;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

/**
 * Purpose of this test is verify correct behavior of Zaas client
 * as a part of application running on mainframe
 */
public class IntegratedZaasClientTest {

    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private final static String INVALID_USERNAME = "incorrectUser";
    private final static String INVALID_PASSWORD = "incorrectPassword";

    @BeforeEach
    public void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    /**
     * This method is testing a communication between discoverable client application
     * using Zaas client and gateway service on mainframe. Main goal is to test correct
     * configuration of SSL, specifically SAF keyring support.
     */
    @Test
    public void loginWithValidCredentials() {
        URI uri = HttpRequestUtils.getUriFromGateway("/api/v1/discoverableclient/zaasClient");
        LoginRequest loginRequest = new LoginRequest(USERNAME, PASSWORD);

        given()
            .contentType(JSON)
            .body(loginRequest)
            .when()
            .post(uri)
            .then()
            .statusCode(is(SC_OK))
            .body(not(isEmptyString()));
    }

    /**
     * This method is testing a communication between discoverable client application
     * using Zaas client and gateway service on mainframe. Main goal is to test correct
     * configuration of SSL, specifically SAF keyring support.
     */
    @Test
    public void invalidCredentials() {
        URI uri = HttpRequestUtils.getUriFromGateway("/api/v1/discoverableclient/zaasClient");
        LoginRequest loginRequest = new LoginRequest(INVALID_USERNAME, INVALID_PASSWORD);

        given()
            .contentType(JSON)
            .body(loginRequest)
            .when()
            .post(uri)
            .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(is("Invalid username or password"));
    }
}
