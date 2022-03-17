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

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithTwoServices;
import org.zowe.apiml.acceptance.netflix.MetadataBuilder;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.SslContextConfigurer;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.reset;

/**
 * This test verifies that the token or client certificate was exchanged. The input is a valid apimlJwtToken/client certificate.
 * The output to be tested is the Zosmf token.
 */
@AcceptanceTest
@TestPropertySource(properties = {"apiml.security.auth.provider=zosmf", "spring.profiles.active=debug", "apiml.security.x509.externalMapperUrl="})
class ZosmfSchemeTest extends AcceptanceTestWithTwoServices {
    @Value("${server.ssl.keyStorePassword:password}")
    private char[] keystorePassword;
    @Value("${server.ssl.keyStore}")
    private String keystore;
    private final String clientKeystore = "../keystore/client_cert/client-certs.p12";

    @Nested
    class GivenClientCertificate {
        @BeforeEach
        void setUp() throws Exception {
            SslContextConfigurer configurer = new SslContextConfigurer(keystorePassword, clientKeystore, keystore);
            SslContext.prepareSslAuthentication(configurer);
            applicationRegistry.clearApplications();
            MetadataBuilder defaultBuilder = MetadataBuilder.defaultInstance();
            defaultBuilder.withZosmf();
            applicationRegistry.addApplication(serviceWithDefaultConfiguration, defaultBuilder, false);
            applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
            reset(mockClient);
        }

        @Nested
        class WhenClientAuthInExtendedKeyUsage {
            // TODO: add checks for transformation once X509 -> zosmf is implemented
            @Test
            void thenOk() throws IOException {

                mockValid200HttpResponse();
                given()
                    .config(SslContext.clientCertUser)
                    .when()
                    .get(basePath + serviceWithDefaultConfiguration.getPath())
                    .then()
                    .statusCode(is(HttpStatus.SC_OK));
            }
        }

        /**
         * When client certificate from request does not have extended key usage set correctly and can't be used for
         * client authentication then request fails with response code 400 - BAD REQUEST
         */
        @Nested
        class WhenNoClientAuthInExtendedKeyUsage {
            @Test
            void thenBadRequest() {
                given()
                    .config(SslContext.apimlRootCert)
                    .when()
                    .get(basePath + serviceWithDefaultConfiguration.getPath())
                    .then()
                    .statusCode(is(HttpStatus.SC_BAD_REQUEST));
            }
        }
    }
}
