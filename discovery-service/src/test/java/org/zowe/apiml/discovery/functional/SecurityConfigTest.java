/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.functional;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.discovery.DiscoveryServiceApplication;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.SslContextConfigurer;

import static io.restassured.RestAssured.given;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    classes = DiscoveryServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles({"https"})
public class SecurityConfigTest {

    @BeforeAll
    static void init() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        SslContextConfigurer configurer = new SslContextConfigurer("password".toCharArray(), "../keystore/client_cert/client-certs.p12", "../keystore/localhost/localhost.keystore.p12");
        SslContext.prepareSslAuthentication(configurer);
    }

    private String getUri(String hostname, int port) {
        return String.format("%s://%s:%d/%s", "https", hostname, port, "eureka/apps");
    }

    @Nested
    @TestPropertySource(
        properties = {
            "apiml.security.ssl.verifySslCertificatesOfServices=false"
        }
    )
    @DirtiesContext
    class GivenDisabledSSLVerification {

        @Value("${apiml.service.hostname:localhost}")
        String hostname;
        @LocalServerPort
        int port;

        @Test
        void thenDoNotRequireAuth() {
            given()
                .get(getUri(hostname, port))
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value());
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

        @Test
        void whenNoClientCertificate_thenReturnUnauthorized() {
            given()
                .get(getUri(hostname, port))
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.FORBIDDEN.value());
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
    }
}
