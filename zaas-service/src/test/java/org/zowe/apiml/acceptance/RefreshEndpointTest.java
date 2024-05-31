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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.zowe.apiml.product.web.HttpConfig;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.zaas.ZaasApplication;
import org.zowe.apiml.zaas.security.mapping.AuthenticationMapper;

import javax.net.ssl.SSLContext;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.core.Is.is;

@SpringBootTest(classes = ZaasApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@MockBean(name = "x509Mapper", classes = AuthenticationMapper.class)
public class RefreshEndpointTest {

    private final static String USERNAME = "USER";
    private final static char[] PASSWORD = "validPassword".toCharArray();

    @LocalServerPort
    private int port;

    @Value("${apiml.service.hostname:localhost}")
    private String hostname;

    private RestAssuredConfig restAssuredConfig;

    @Autowired
    HttpConfig httpConfig;

    @BeforeEach
    void setUp() {
        initializeTls();
    }

    private String getBasePath() {
        return "https://" + hostname + ":" + port;
    }

    @Nested
    @SuppressWarnings("squid:S2699") // sonar doesn't identify the restAssured assertions
    class GivenIllegalAccessModes {

        @Test
        void noClientCertificateGivesForbidden() {
            //@formatter:off
            given().config(RestAssuredConfig.newConfig().sslConfig(new SSLConfig()))
                .when().post(getBasePath() + "/zaas/api/v1/auth/refresh") //REFRESH_URL
                .then().statusCode(is(SC_FORBIDDEN));
            //@formatter:on
        }

        @Test
        void clientCertificateWithoutTokenGivesUnauthorized() {
            LoginRequest loginRequest = new LoginRequest(USERNAME, PASSWORD);

            //@formatter:off
            given().config(restAssuredConfig)
                .when()
                    .contentType(JSON)
                    .body(loginRequest)
                    .post( getBasePath() + "/zaas/api/v1/auth/refresh")
                .then()
                    .statusCode(is(SC_UNAUTHORIZED));
            //@formatter:on
        }

    }

    public synchronized void initializeTls() {
        if (restAssuredConfig == null) {
            SSLContext sslContext = httpConfig.secureSslContext();
            restAssuredConfig =  RestAssuredConfig.newConfig().sslConfig(new SSLConfig().sslSocketFactory(new SSLSocketFactory(sslContext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)));
        }
    }

}
