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

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.zowe.apiml.enable.register.RegisterToApiLayer;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.SslContextConfigurer;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_METHOD_NOT_ALLOWED;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@AcceptanceTest
@Import(ReactiveAuthenticationControllerTests.MockRegisterToApiLayerConfig.class)
class ReactiveAuthenticationControllerTests extends AcceptanceTestWithMockServices {

    private static final String REFRESH_ENDPOINT = "/gateway/api/v1/auth/refresh";
    private static final String LOGIN_ENDPOINT = "/gateway/api/v1/auth/login";
    private static final String AUTH_COOKIE = "apimlAuthenticationToken";
    private static final String DISTRIBUTE_INVALIDATE_ENDPOINT = "/gateway/api/v1/auth/distribute";
    private static final String INVALIDATE_JWT_ENDPOINT = "/gateway/api/v1/auth/invalidate";

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
            .contentType(ContentType.JSON)
            .body("""
                {
                    "username": "USER",
                    "password": "validPassword"
                }
            """)
            .log().all()
        .when()
            .post(URI.create(basePath + LOGIN_ENDPOINT))
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
    void whenRefreshTokenWithoutCert_then403() {
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

    @Test
    void whenRefreshTokenWithCert_thenSuccess() {
        var token = login();

        var newToken = given()
            .config(SslContext.clientCertApiml)
            .cookie(AUTH_COOKIE, token)
        .when()
            .post(URI.create(basePath + REFRESH_ENDPOINT))
        .then()
            .statusCode(200)
            .cookie(AUTH_COOKIE)
        .extract()
            .cookie(AUTH_COOKIE);

        assertNotEquals(token, newToken);
    }

    @Test
    void whenDistributeInvalidate_thenRequireCertificateAuthentication() {
        given()
            .log()
            .all()
        .when()
            .get(URI.create(basePath + DISTRIBUTE_INVALIDATE_ENDPOINT + "/instanceId"))
        .then()
            .statusCode(403);
    }

    @Test
    void whenDistributeInvalidate_withCert_thenSuccess() {
        given()
            .config(SslContext.clientCertApiml)
        .when()
            .get(URI.create(basePath + DISTRIBUTE_INVALIDATE_ENDPOINT + "/instanceId"))
        .then()
            .statusCode(204);
    }

    @Test
    void whenInvalidateJwt_thenRequireCertificateAuthentication() {
        var token = login();

        given()
            .log()
            .all()
        .when()
            .delete(URI.create(basePath + INVALIDATE_JWT_ENDPOINT + "/" + token))
        .then()
            .statusCode(403);
    }

    @Test
    void whenInvalidate_wrongMethod_thenFail() {
        var token = login();

        given()
        .when()
            .get(URI.create(basePath + INVALIDATE_JWT_ENDPOINT + "/" + token))
        .then()
            .statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    void whenInvalidateJwt_withCert_thenSuccess() {
        var token = login();

        given()
            .config(SslContext.clientCertApiml)
        .when()
            .delete(URI.create(basePath + INVALIDATE_JWT_ENDPOINT + "/" + token))
        .then()
            .statusCode(200);
    }
    @TestConfiguration
    public static class MockRegisterToApiLayerConfig {
        @Bean
        public RegisterToApiLayer registerToApiLayer() {
            return mock(RegisterToApiLayer.class);
        }
    }
}
