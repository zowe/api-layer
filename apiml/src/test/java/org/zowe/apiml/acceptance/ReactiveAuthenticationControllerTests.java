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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.SslContextConfigurer;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@AcceptanceTest
class ReactiveAuthenticationControllerTests extends AcceptanceTestWithMockServices {

    private static final String REFRESH_ENDPOINT = "/gateway/api/v1/auth/refresh";
    private static final String AUTH_COOKIE = "apimlAuthenticationToken";

    @Value("${server.ssl.keyPassword}")
    char[] password;
    @Value("${server.ssl.keyStore}")
    String client_cert_keystore;
    @Value("${server.ssl.keyStore}")
    String keystore;

    @BeforeEach
    void setUp() throws Exception {
        mockZosmfSuccess();
        SslContextConfigurer configurer = new SslContextConfigurer(password, client_cert_keystore, keystore);
        SslContext.prepareSslAuthentication(configurer);
    }

    private String login() {
        return given()
            .body("""
                {
                    "username": "USER",
                    "password": "validPassword"
                }
            """)
            .log().all()
        .when()
            .post(URI.create(basePath + "/gateway/api/v1/auth/login"))
        .then()
            .statusCode(204)
            .cookie(AUTH_COOKIE)
        .extract()
            .cookie(AUTH_COOKIE);
    }

    @Test
    void whenLoginWithBody_thenSuccess() {
        var token = login();
        assertNotNull(token);
    }

    @Test
    void whenRefreshPATWithoutCert_then403() {
        given()
        .when()
            .post(URI.create(basePath + REFRESH_ENDPOINT))
        .then()
            .statusCode(403);
    }

    @Test
    void whenWrongMethod_thenFail() {
        given()
        .when()
            .get(URI.create(basePath + REFRESH_ENDPOINT))
        .then()
            .statusCode(405);
    }

}
