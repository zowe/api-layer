/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.security.common.verify.CertificateValidator;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.SslContextConfigurer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(Lifecycle.PER_CLASS)
public class InMemoryFunctionalTest {

    @Value("${server.ssl.keyPassword}")
    char[] password;
    @Value("${server.ssl.keyStore}")
    String client_cert_keystore;
    @Value("${server.ssl.keyStore}")
    String keystore;

    @Value("${apiml.service.hostname:localhost}")
    String hostname;

    @LocalServerPort
    int port;

    public static final String CLIENT_AUTH_CERTIFICATE_HEADER = "Client-Cert";

    String contextPath = "/cachingservice/api/v1";

    String getUri(String endpoint) {
        return String.format("https://%s:%s%s%s", hostname, port, contextPath, endpoint);
    }

    @MockitoBean
    private CertificateValidator certificateValidator;

    private static final String MOCK_FORWARDED_CERT_HEADER = """
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
    void init() {
        SslContext.reset();
    }

    @BeforeEach
    void setup() throws Exception {
        SslContextConfigurer configurer = new SslContextConfigurer(password, client_cert_keystore, keystore);
        SslContext.prepareSslAuthentication(configurer);
        org.mockito.Mockito.when(certificateValidator.isForwardingEnabled()).thenReturn(true);
        org.mockito.Mockito.when(certificateValidator.hasGatewayChain(org.mockito.Mockito.any())).thenReturn(true);
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class WhenCallingByTrustedClient {

        @Test
        @Order(1)
        void createEntry() throws Exception {
            KeyValue keyValue = new KeyValue("first-key", "anyValue");
            ObjectMapper mapper = new ObjectMapper();
            given().config(SslContext.clientCertApiml)
                .body(mapper.writeValueAsString(keyValue))
                .header("Content-type", "application/json")
                .header("X-CS-Service-ID", "service1")
                .header(CLIENT_AUTH_CERTIFICATE_HEADER, MOCK_FORWARDED_CERT_HEADER)
                .post(getUri("/cache"))
                .then()
                .statusCode(HttpStatus.CREATED.value());
        }

        @Test
        @Order(2)
        void readAllEntries() {
            given().config(SslContext.clientCertApiml)
                .header("Content-type", "application/json")
                .header("X-CS-Service-ID", "service1")
                .header(CLIENT_AUTH_CERTIFICATE_HEADER, MOCK_FORWARDED_CERT_HEADER)
                .get(getUri("/cache"))
                .then()
                .body("first-key.key", is("first-key"))
                .statusCode(HttpStatus.OK.value());
        }

        @Test
        @Order(3)
        void readEntry() {
            given().config(SslContext.clientCertApiml)
                .header("Content-type", "application/json")
                .header("X-CS-Service-ID", "service1")
                .header(CLIENT_AUTH_CERTIFICATE_HEADER, MOCK_FORWARDED_CERT_HEADER)
                .get(getUri("/cache/first-key"))
                .then()
                .body("value", is("anyValue"))
                .statusCode(HttpStatus.OK.value());
        }

        @Test
        @Order(4)
        void updateEntry() throws Exception {
            KeyValue keyValue = new KeyValue("first-key", "newValue");
            ObjectMapper mapper = new ObjectMapper();

            given().config(SslContext.clientCertApiml)
                .body(mapper.writeValueAsString(keyValue))
                .header("Content-type", "application/json")
                .header("X-CS-Service-ID", "service1")
                .header(CLIENT_AUTH_CERTIFICATE_HEADER, MOCK_FORWARDED_CERT_HEADER)
                .put(getUri("/cache"))
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            given().config(SslContext.clientCertApiml)
                .header("Content-type", "application/json")
                .header("X-CS-Service-ID", "service1")
                .header(CLIENT_AUTH_CERTIFICATE_HEADER, MOCK_FORWARDED_CERT_HEADER)
                .get(getUri("/cache/first-key"))
                .then()
                .body("value", is("newValue"))
                .statusCode(HttpStatus.OK.value());
        }

        @Test
        @Order(5)
        void deleteEntry() {
            given().config(SslContext.clientCertApiml)
                .header("Content-type", "application/json")
                .header("X-CS-Service-ID", "service1")
                .header(CLIENT_AUTH_CERTIFICATE_HEADER, MOCK_FORWARDED_CERT_HEADER)
                .delete(getUri("/cache/first-key"))
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
        }
    }

    @Nested
    class WhenClientIsNotTrusted {
        @Test
        @Order(6)
        void responseIsForbidden() {
            given().config(SslContext.clientCertUnknownUser)
                .header("Content-type", "application/json")
                .header("X-CS-Service-ID", "service1")
                .get(getUri("/cache"))
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
        }
    }
}
