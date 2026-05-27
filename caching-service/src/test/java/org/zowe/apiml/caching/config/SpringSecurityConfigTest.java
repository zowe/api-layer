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
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.caching.CachingServiceApplication;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.SslContextConfigurer;

import java.util.Base64;

import static io.restassured.RestAssured.given;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SpringSecurityConfigTest {

    private static final String USER = "user";
    private static final String PASSWORD = "password";

    private static final String VALID_BASIC_AUTH = "Basic " + Base64.getEncoder().encodeToString((USER + ":" + PASSWORD).getBytes());
    private static final String INVALID_BASIC_AUTH = "Basic " + Base64.getEncoder().encodeToString((USER + ":invalidPassword").getBytes());
    private static final String X_CS_SERVICE_ID = "X-CS-Service-ID";

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
    @DirtiesContext
    @TestPropertySource(
        properties = {
            "apiml.service.ssl.verifySslCertificatesOfServices=false",
            "apiml.service.http.userId=user",
            "apiml.service.http.password=password"
        }
    )
    @SpringBootTest(
        classes = CachingServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
    )
    class GivenDisabledSSLVerificationAndCachingCredentials {

        @Value("${apiml.service.hostname:localhost}")
        String hostname;

        @LocalServerPort
        int port;

        @Nested
        class givenNoBasicAuth {

            @Test
            void thenReturnForbidden() {
                given()
                    .header(new Header(X_CS_SERVICE_ID, "apimtst"))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.FORBIDDEN.value());
            }

        }

        @Nested
        class givenBasicAuth {

            @Test
            void whenValidBasicAuth_thenReturnSuccess() {
                given()
                    .header(new Header(X_CS_SERVICE_ID, "apimtst"))
                    .header(new Header(HttpHeaders.AUTHORIZATION, VALID_BASIC_AUTH))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.OK.value());
            }

            @Test
            void whenInvalidBasicAuth_thenReturnUnauthorized() {
                given()
                    .header(new Header(X_CS_SERVICE_ID, "apimtst"))
                    .header(new Header(HttpHeaders.AUTHORIZATION, INVALID_BASIC_AUTH))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.UNAUTHORIZED.value());
            }
        }
    }

    @Nested
    @DirtiesContext
    @TestPropertySource(
        properties = {
            "apiml.service.ssl.verifySslCertificatesOfServices=false"
        }
    )
    @SpringBootTest(
        classes = CachingServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
    )
    class GivenDisabledSSLVerificationAndNoCachingCredentials {

        @Value("${apiml.service.hostname:localhost}")
        String hostname;

        @LocalServerPort
        int port;

        @Nested
        class givenNoBasicAuth {

            @Test
            void thenReturnForbidden() {
                given()
                    .header(new Header(X_CS_SERVICE_ID, "apimtst"))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.FORBIDDEN.value());
            }

        }

        @Nested
        class givenBasicAuth {

            @Test
            void whenValidBasicAuth_thenReturnUnauthorized() {
                given()
                    .header(new Header(X_CS_SERVICE_ID, "apimtst"))
                    .header(new Header(HttpHeaders.AUTHORIZATION, VALID_BASIC_AUTH))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.UNAUTHORIZED.value());
            }

            @Test
            void whenInvalidBasicAuth_thenReturnUnauthorized() {
                given()
                    .header(new Header(X_CS_SERVICE_ID, "apimtst"))
                    .header(new Header(HttpHeaders.AUTHORIZATION, INVALID_BASIC_AUTH))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.UNAUTHORIZED.value());
            }
        }
    }

    @Nested
    @DirtiesContext
    @TestPropertySource(
        properties = {
            "apiml.service.ssl.verifySslCertificatesOfServices=true",
            "apiml.service.http.userId=user",
            "apiml.service.http.password=password"
        }
    )
    @SpringBootTest(
        classes = CachingServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
    )
    class GivenEnabledSSLVerificationAndCachingCredentials {

        @Value("${apiml.service.hostname:localhost}")
        String hostname;

        @LocalServerPort
        int port;

        @Nested
        class givenNoClientCertificate {

            @Test
            void whenNoBasicAuth_thenReturnForbidden() {
                given()
                    .header(new Header(X_CS_SERVICE_ID, "apimtst"))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.FORBIDDEN.value());
            }

            @Test
            void whenValidBasicAuth_thenReturnForbidden() {
                given()
                    .header(new Header(X_CS_SERVICE_ID, "apimtst"))
                    .header(new Header(HttpHeaders.AUTHORIZATION, VALID_BASIC_AUTH))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.FORBIDDEN.value());
            }

            @Test
            void whenInvalidBasicAuth_thenReturnForbidden() {
                given()
                    .header(new Header(X_CS_SERVICE_ID, "apimtst"))
                    .header(new Header(HttpHeaders.AUTHORIZATION, INVALID_BASIC_AUTH))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.FORBIDDEN.value());
            }
        }

        @Nested
        class givenClientCertificate {

            @Test
            void whenNoBasicAuth_thenReturnSuccess() {
                given()
                    .config(SslContext.clientCertApiml)
                    .header(new Header(X_CS_SERVICE_ID, "apimtst"))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.OK.value());
            }

            @Test
            void whenValidBasicAuth_thenReturnSuccess() {
                given()
                    .config(SslContext.clientCertApiml)
                    .header(new Header(X_CS_SERVICE_ID, "apimtst"))
                    .header(new Header(HttpHeaders.AUTHORIZATION, VALID_BASIC_AUTH))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.OK.value());
            }

            @Test
            void whenInvalidBasicAuth_thenReturnSuccess() {
                given()
                    .config(SslContext.clientCertApiml)
                    .header(new Header(X_CS_SERVICE_ID, "apimtst"))
                    .header(new Header(HttpHeaders.AUTHORIZATION, INVALID_BASIC_AUTH))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.OK.value());
            }
        }

    }

    @Nested
    @DirtiesContext
    @TestPropertySource(
        properties = {
            "apiml.service.ssl.verifySslCertificatesOfServices=true"
        }
    )
    @SpringBootTest(
        classes = CachingServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
    )
    class GivenEnabledSSLVerificationAndNoCachingCredentials {

        @Value("${apiml.service.hostname:localhost}")
        String hostname;

        @LocalServerPort
        int port;

        @Nested
        class givenNoClientCertificate {

            @Test
            void whenNoBasicAuth_thenReturnForbidden() {
                given()
                    .header(new Header(X_CS_SERVICE_ID, "apimtst"))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.FORBIDDEN.value());
            }

            @Test
            void whenValidBasicAuth_thenReturnForbidden() {
                given()
                    .header(new Header(X_CS_SERVICE_ID, "apimtst"))
                    .header(new Header(HttpHeaders.AUTHORIZATION, VALID_BASIC_AUTH))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.FORBIDDEN.value());
            }

            @Test
            void whenInvalidBasicAuth_thenReturnForbidden() {
                given()
                    .header(new Header(X_CS_SERVICE_ID, "apimtst"))
                    .header(new Header(HttpHeaders.AUTHORIZATION, INVALID_BASIC_AUTH))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.FORBIDDEN.value());
            }
        }

        @Nested
        class givenClientCertificate {

            @Test
            void whenNoBasicAuth_thenReturnSuccess() {
                given()
                    .config(SslContext.clientCertApiml)
                    .header(new Header(X_CS_SERVICE_ID, "apimtst"))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.OK.value());
            }

            @Test
            void whenValidBasicAuth_thenReturnSuccess() {
                given()
                    .config(SslContext.clientCertApiml)
                    .header(new Header(X_CS_SERVICE_ID, "apimtst"))
                    .header(new Header(HttpHeaders.AUTHORIZATION, VALID_BASIC_AUTH))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.OK.value());
            }

            @Test
            void whenInvalidBasicAuth_thenReturnSuccess() {
                given()
                    .config(SslContext.clientCertApiml)
                    .header(new Header(X_CS_SERVICE_ID, "apimtst"))
                    .header(new Header(HttpHeaders.AUTHORIZATION, INVALID_BASIC_AUTH))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.OK.value());
            }
        }

    }

}
