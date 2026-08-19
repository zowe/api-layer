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
@ActiveProfiles({"test", "https"})
public class SecurityConfigTest {

    @BeforeAll
    static void init() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        SslContextConfigurer configurer = new SslContextConfigurer("password".toCharArray(), "../keystore/client/client-certs.p12", "../keystore/service/service.keystore.p12");
        SslContext.prepareSslAuthentication(configurer);
    }

    private String getUri(String hostname, int port) {
        return String.format("%s://%s:%d/%s", "https", hostname, port, "eureka/apps");
    }

    @Nested
    @TestPropertySource(
        properties = {
            "apiml.security.ssl.verifySslCertificatesOfServices=false",
            "apiml.discovery.userid=eureka",
            "apiml.discovery.password=password"
        }
    )
    @DirtiesContext
    class GivenDisabledSSLVerification {

        private static final String EUREKA_USERID = "eureka";
        private static final String EUREKA_PASSWORD = "password";

        @Value("${apiml.service.hostname:localhost}")
        String hostname;
        @LocalServerPort
        int port;

        @Test
        void whenNoBasicAuth_thenReturnUnauthorized() {
            given()
                .get(getUri(hostname, port))
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
        }

        @Test
        void whenBasicAuth_thenReturnOk() {
            given()
                .auth().basic(EUREKA_USERID, EUREKA_PASSWORD)
                .get(getUri(hostname, port))
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value());
        }
    }

    @Nested
    @TestPropertySource(
        properties = {
            "apiml.security.ssl.verifySslCertificatesOfServices=false"
        }
    )
    @DirtiesContext
    class GivenDisabledSSLVerificationWithoutCredentials {

        @Value("${apiml.service.hostname:localhost}")
        String hostname;
        @LocalServerPort
        int port;

        @Test
        void thenStillRequireAuth() {
            // Authentication is never disabled: without configured credentials basic auth cannot succeed.
            given()
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
