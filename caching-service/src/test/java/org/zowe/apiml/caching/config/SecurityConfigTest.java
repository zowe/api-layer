/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.config;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.caching.CachingServiceApplication;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.SslContextConfigurer;

import static io.restassured.RestAssured.given;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SecurityConfigTest {

    @BeforeAll
    static void init() throws Exception {
        SslContext.reset();
        RestAssured.useRelaxedHTTPSValidation();
        SslContextConfigurer configurer = new SslContextConfigurer("password".toCharArray(), "../keystore/client_cert/client-certs.p12", "../keystore/localhost/localhost.keystore.p12");
        SslContext.prepareSslAuthentication(configurer);
    }

    private String getUri(String hostname, int port) {
        return String.format("%s://%s:%d/%s", "https", hostname, port, "cachingservice/api/v1/cache");
    }

    @Nested
    @TestPropertySource(
        properties = {
            "apiml.service.ssl.verifySslCertificatesOfServices=false"
        }
    )
    @SpringBootTest(
        classes = CachingServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
    )
    class GivenDisabledSSLVerification {

        @Value("${apiml.service.hostname:localhost}")
        String hostname;
        @LocalServerPort
        int port;

        @Test
        void thenDoNotRequireAuth() {
            given()
                .header(new Header("X-CS-Service-ID", "apimtst"))
                .get(getUri(hostname, port))
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value());
        }
    }

    @Nested
    @TestPropertySource(
        properties = {
            "apiml.service.ssl.verifySslCertificatesOfServices=true"
        }
    )
    @SpringBootTest(
        classes = CachingServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
    )
    class GivenEnabledSSLVerification {

        @Value("${apiml.service.hostname:localhost}")
        String hostname;
        @LocalServerPort
        int port;

        @Test
        void whenNoClientCertificate_thenReturnUnauthorized() {
            given()
                .header(new Header("X-CS-Service-ID", "apimtst"))
                .get(getUri(hostname, port))
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void whenClientCertificate_thenReturnOk() {

            given()
                .config(SslContext.clientCertApiml)
                .header(new Header("X-CS-Service-ID", "apimtst"))
                .get(getUri(hostname, port))
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value());
        }
    }
}
