/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.security;

import io.restassured.RestAssured;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.zowe.apiml.apicatalog.ApiCatalogApplication;
import org.zowe.apiml.apicatalog.swagger.ApiDocService;
import org.zowe.apiml.security.client.service.GatewaySecurity;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.SslContextConfigurer;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    classes = ApiCatalogApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class SecurityConfigTest {


    @BeforeAll
    static void init() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        SslContextConfigurer configurer = new SslContextConfigurer("password".toCharArray(), "../keystore/client_cert/client-certs.p12", "../keystore/localhost/localhost.keystore.p12");
        SslContext.prepareSslAuthentication(configurer);

    }

    private String getUri(String hostname, int port) {
        return String.format("%s://%s:%d/%s", "https", hostname, port, "apicatalog/apidoc/service1");
    }

    @Nested
    @TestPropertySource(
        properties = {
            "apiml.security.ssl.verifySslCertificatesOfServices=false",
            "apiml.security.ssl.nonStrictVerifySslCertificatesOfServices=true"
        }
    )
    @DirtiesContext
    class GivenDisabledSSLVerification {

        @Value("${apiml.service.hostname:localhost}")
        String hostname;
        @LocalServerPort
        int port;

        @Test
        void whenClientCertificate_thenReturnUnauthorized() {
            given()
                .config(SslContext.clientCertApiml)
                .get(getUri(hostname, port))
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
        }
    }

    @Nested
    @TestPropertySource(
        properties = {
            "apiml.security.ssl.verifySslCertificatesOfServices=true"
        }
    )
    @DirtiesContext
    class GivenEnabledSSLVerification {

        @Value("${apiml.service.hostname:localhost}")
        String hostname;
        @LocalServerPort
        int port;

        @MockitoBean
        GatewaySecurity gatewaySecurity;

        @MockitoBean
        ApiDocService apiDocService;

        @BeforeEach
        void setup() {
            when(apiDocService.retrieveDefaultApiDoc(any())).thenReturn(Mono.just("{}"));
        }

        @Test
        void whenNoClientCertificateButBasicAuth_thenReturnOk() {
            when(gatewaySecurity.login(any(), any(), any())).thenReturn(Optional.of("token"));
            given()
                .header("Authorization", "Basic dXNlcjpwYXNz")
                .get(getUri(hostname, port))
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value());
        }

        @Test
        void whenClientCertificate_thenReturnOk() {
            given()
                .config(SslContext.clientCertApiml)
                .get(getUri(hostname, port))
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value());
        }

        @Test
        void whenNoCredentials_thenReturnUnauthorized() {

            given()
                .get(getUri(hostname, port))
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
        }
    }
}
