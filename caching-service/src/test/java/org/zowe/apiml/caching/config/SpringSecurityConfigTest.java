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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.zowe.apiml.caching.CachingServiceApplication;
import org.zowe.apiml.security.common.verify.CertificateValidator;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.SslContextConfigurer;

import java.util.Base64;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SpringSecurityConfigTest {

    private static final String USER = "user";
    private static final String PASSWORD = "password";

    private static final String VALID_BASIC_AUTH = "Basic " + Base64.getEncoder().encodeToString((USER + ":" + PASSWORD).getBytes());
    private static final String INVALID_BASIC_AUTH = "Basic " + Base64.getEncoder().encodeToString((USER + ":invalidPassword").getBytes());
    private static final String X_CS_SERVICE_ID = "Client-Cert";
    private static final String MOCK_FORWARDED_CERT = """
        MIID7zCCAtegAwIBAgIED0TPEjANBgkqhkiG9w0BAQsFADB6MQswCQYDVQQGEwJD
        WjEPMA0GA1UECBMGUHJhZ3VlMQ8wDQYDVQQHEwZQcmFndWUxFDASBgNVBAoTC1pv
        d2UgU2FtcGxlMRwwGgYDVQQLExNBUEkgTWVkaWF0aW9uIExheWVyMRUwEwYDVQQD
        Ewxab3dlIFNlcnZpY2UwHhcNMTgxMjA3MTQ1NzIyWhcNMjgxMjA0MTQ1NzIyWjB6
        MQswCQYDVQQGEwJDWjEPMA0GA1UECBMGUHJhZ3VlMQ8wDQYDVQQHEwZQcmFndWUx
        FDASBgNVBAoTC1pvd2UgU2FtcGxlMRwwGgYDVQQLExNBUEkgTWVkaWF0aW9uIExh
        eWVyMRUwEwYDVQQDEwxab3dlIFNlcnZpY2UwggEiMA0GCSqGSIb3DQEBAQUAA4IB
        DwAwggEKAoIBAQC6Orc/EJ5/t2qam1DiYU/xVbHaQrjd6uvpj2HTvOOohtFZ7/Kx
        yMAezgB8DBR4+77qXXsdP9ngnTl/i22yGwvo7Tlz6dhnQLnks7VFr1eGGC2ks+rL
        BJsF/RQexmONG9ddexWD8SOYoW9RRapQqETbcllxOenvzXruOEzaXhMazkK9Cg+J
        ucNb9HcfhIM0rjLZhqG8Gc8dAtCcxF/xHlVyFQq8fr4u2p/wGmARM14iZeQltQV7
        F3gxmw3djfcNM5S3tirPrHlZb76ZmmQEn4QiLSP198Lm+4QKAOw1dUpMf4eELO4c
        EFUHXQUCHLWc5NztZxWW40NrDbZEjcRI5ah7AgMBAAGjfTB7MB0GA1UdJQQWMBQG
        CCsGAQUFBwMCBggrBgEFBQcDATAOBgNVHQ8BAf8EBAMCBPAwKwYDVR0RBCQwIoIV
        bG9jYWxob3N0LmxvY2FsZG9tYWlugglsb2NhbGhvc3QwHQYDVR0OBBYEFHL1ygBb
        UCI/ktdk3TgQA6EJlATIMA0GCSqGSIb3DQEBCwUAA4IBAQBHALBlFf0P1TBR1MHQ
        vXYDFAW+PiyF7zP0HcrvQTAGYhF7uJtRIamapjUdIsDVbqY0RhoFnBOu8ti2z0pW
        djw47f3X/yj98n+J2aYcO64Ar+ovx93P01MA8+Mz1u/LwXk4pmrbUIcOEtyNu+vT
        a0jDobC++3Zfv5Y+iD2M8L+jacSMZNCqQByhKtTkAICXg9LMccx4XLYtJ65zGP2h
        4TEK0MMfO2G1/vUmdb3tq17zKdukj3MUS254mENCck7ioNFR0Cc9lzuSHyBrdb0x
        M/iHeamNblckK/r1roDjhCAQz9DtmETad/o7qGNFxDTRRShRV9Lww0fFB7PaV7u/
        VPx2
        """.replaceAll("\\s+", "");

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

        @MockitoBean
        CertificateValidator certificateValidator;

        @Value("${apiml.service.hostname:localhost}")
        String hostname;

        @LocalServerPort
        int port;

        @BeforeEach
        void setup() {
            when(certificateValidator.isForwardingEnabled()).thenReturn(true);
            when(certificateValidator.hasGatewayChain(any())).thenReturn(true);
        }

        @Nested
        class givenNoBasicAuth {

            @Test
            void thenReturnForbidden() {
                given()
                    .header(new Header(X_CS_SERVICE_ID, MOCK_FORWARDED_CERT))
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
                    .header(new Header(X_CS_SERVICE_ID, MOCK_FORWARDED_CERT))
                    .header(new Header(HttpHeaders.AUTHORIZATION, VALID_BASIC_AUTH))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.UNAUTHORIZED.value());
            }

            @Test
            void whenInvalidBasicAuth_thenReturnUnauthorized() {
                given()
                    .header(new Header(X_CS_SERVICE_ID, MOCK_FORWARDED_CERT))
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

        @MockitoBean
        CertificateValidator certificateValidator;

        @Value("${apiml.service.hostname:localhost}")
        String hostname;

        @LocalServerPort
        int port;

        @BeforeEach
        void setup() {
            when(certificateValidator.isForwardingEnabled()).thenReturn(true);
            when(certificateValidator.hasGatewayChain(any())).thenReturn(true);
        }

        @Nested
        class givenNoBasicAuth {

            @Test
            void thenReturnForbidden() {
                given()
                    .header(new Header(X_CS_SERVICE_ID, MOCK_FORWARDED_CERT))
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
                    .header(new Header(X_CS_SERVICE_ID, MOCK_FORWARDED_CERT))
                    .header(new Header(HttpHeaders.AUTHORIZATION, VALID_BASIC_AUTH))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.UNAUTHORIZED.value());
            }

            @Test
            void whenInvalidBasicAuth_thenReturnUnauthorized() {
                given()
                    .header(new Header(X_CS_SERVICE_ID, MOCK_FORWARDED_CERT))
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

        @MockitoBean
        CertificateValidator certificateValidator;

        @Value("${apiml.service.hostname:localhost}")
        String hostname;

        @LocalServerPort
        int port;

        @BeforeEach
        void setup() {
            when(certificateValidator.isForwardingEnabled()).thenReturn(true);
            when(certificateValidator.hasGatewayChain(any())).thenReturn(true);
        }

        @Nested
        class givenNoClientCertificate {

            @Test
            void whenNoBasicAuth_thenReturnForbidden() {
                given()
                    .header(new Header(X_CS_SERVICE_ID, MOCK_FORWARDED_CERT))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.FORBIDDEN.value());
            }

            @Test
            void whenValidBasicAuth_thenReturnForbidden() {
                given()
                    .header(new Header(X_CS_SERVICE_ID, MOCK_FORWARDED_CERT))
                    .header(new Header(HttpHeaders.AUTHORIZATION, VALID_BASIC_AUTH))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.FORBIDDEN.value());
            }

            @Test
            void whenInvalidBasicAuth_thenReturnForbidden() {
                given()
                    .header(new Header(X_CS_SERVICE_ID, MOCK_FORWARDED_CERT))
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
                    .header(new Header(X_CS_SERVICE_ID, MOCK_FORWARDED_CERT))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.OK.value());
            }

            @Test
            void whenValidBasicAuth_thenReturnSuccess() {
                given()
                    .config(SslContext.clientCertApiml)
                    .header(new Header(X_CS_SERVICE_ID, MOCK_FORWARDED_CERT))
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
                    .header(new Header(X_CS_SERVICE_ID, MOCK_FORWARDED_CERT))
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

        @MockitoBean
        CertificateValidator certificateValidator;

        @Value("${apiml.service.hostname:localhost}")
        String hostname;

        @LocalServerPort
        int port;

        @BeforeEach
        void setup() {
            when(certificateValidator.isForwardingEnabled()).thenReturn(true);
            when(certificateValidator.hasGatewayChain(any())).thenReturn(true);
        }

        @Nested
        class givenNoClientCertificate {

            @Test
            void whenNoBasicAuth_thenReturnForbidden() {
                given()
                    .header(new Header(X_CS_SERVICE_ID, MOCK_FORWARDED_CERT))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.FORBIDDEN.value());
            }

            @Test
            void whenValidBasicAuth_thenReturnForbidden() {
                given()
                    .header(new Header(X_CS_SERVICE_ID, MOCK_FORWARDED_CERT))
                    .header(new Header(HttpHeaders.AUTHORIZATION, VALID_BASIC_AUTH))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.FORBIDDEN.value());
            }

            @Test
            void whenInvalidBasicAuth_thenReturnForbidden() {
                given()
                    .header(new Header(X_CS_SERVICE_ID, MOCK_FORWARDED_CERT))
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
                    .header(new Header(X_CS_SERVICE_ID, MOCK_FORWARDED_CERT))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.OK.value());
            }

            @Test
            void whenValidBasicAuth_thenReturnSuccess() {
                given()
                    .config(SslContext.clientCertApiml)
                    .header(new Header(X_CS_SERVICE_ID, MOCK_FORWARDED_CERT))
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
                    .header(new Header(X_CS_SERVICE_ID, MOCK_FORWARDED_CERT))
                    .header(new Header(HttpHeaders.AUTHORIZATION, INVALID_BASIC_AUTH))
                    .get(getUri(hostname, port))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.OK.value());
            }
        }

    }

}
