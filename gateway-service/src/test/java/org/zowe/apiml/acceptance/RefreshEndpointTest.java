/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance;

import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithBasePath;
import org.zowe.apiml.product.web.HttpConfig;
import org.zowe.apiml.security.common.login.LoginRequest;

import javax.net.ssl.SSLContext;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.core.Is.is;

@AcceptanceTest
public class RefreshEndpointTest extends AcceptanceTestWithBasePath {

    private final static String USERNAME = "USER";
    private final static char[] PASSWORD = "validPassword".toCharArray();

    private RestAssuredConfig restAssuredConfig;

    @Autowired
    HttpConfig httpConfig;

    @BeforeEach
    void setUp() {
        initializeTls();
    }

    @Nested
    @SuppressWarnings("squid:S2699") // sonar doesn't identify the restAssured assertions
    class GivenIllegalAccessModes {
        @Test
        void noClientCertificateGivesForbidden() {

            given().config(RestAssuredConfig.newConfig().sslConfig(new SSLConfig()))
                .when()
                .post(basePath + "/gateway/api/v1/auth/refresh") //REFRESH_URL
                .then()
                .statusCode(is(SC_FORBIDDEN));
        }

        @Test
        void clientCertificateWithoutTokenGivesUnauthorized() {
            LoginRequest loginRequest = new LoginRequest(USERNAME, PASSWORD);

            given().config(restAssuredConfig)
                .when()
                .contentType(JSON)
                .body(loginRequest)
                .post(basePath + "/gateway/api/v1/auth/refresh")
                .then()
                .statusCode(is(SC_UNAUTHORIZED));
        }
    }


    public synchronized void initializeTls() {
        if (restAssuredConfig == null) {
            SSLContext sslContext = httpConfig.secureSslContext();
            restAssuredConfig =  RestAssuredConfig.newConfig().sslConfig(new SSLConfig().sslSocketFactory(new SSLSocketFactory(sslContext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)));
        }
    }


}
