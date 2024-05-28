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

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.acceptance.netflix.MetadataBuilder;
import org.zowe.apiml.zaas.security.service.zosmf.ZosmfService;
import org.zowe.apiml.zaas.utils.JWTUtils;
import org.zowe.apiml.security.HttpsConfig;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.SslContextConfigurer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * This test verifies that the token or client certificate was exchanged. The input is a valid apimlJwtToken/client certificate.
 * The output to be tested is the Zosmf token.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "apiml.security.auth.provider=zosmf",
    "spring.profiles.active=debug",
    "apiml.security.x509.enabled=true",
    "apiml.security.x509.externalMapperUrl="
})
class ZosmfSchemeTest {
    @Value("${server.ssl.keyStorePassword:password}")
    private char[] keystorePassword;
    @Value("${server.ssl.keyStore}")
    private String keystore;
    private final String clientKeystore = "../keystore/client_cert/client-certs.p12";
    @Value("${server.ssl.keyAlias:#{null}}")
    private String keyAlias;

    @Autowired
    public ZosmfService zosmfService;


    @Nested
    class GivenClientCertificate {

        public HttpsConfig config;

        @BeforeEach
        void setUp() throws Exception {
            SslContextConfigurer configurer = new SslContextConfigurer(keystorePassword, clientKeystore, keystore);
            SslContext.prepareSslAuthentication(configurer);
            applicationRegistry.clearApplications();

            MetadataBuilder defaultBuilder = MetadataBuilder.defaultInstance();
            defaultBuilder.withZosmf();
            applicationRegistry.addApplication(serviceWithDefaultConfiguration, defaultBuilder, false);
            applicationRegistry.addApplication(serviceWithCustomConfiguration, defaultBuilder, true);
            applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
            reset(mockClient);
            config = HttpsConfig.builder().keyAlias(keyAlias).keyPassword(keystorePassword).keyStore(keystore).build();
        }

        @Nested
        class WhenClientAuthInExtendedKeyUsage {

            void assertResult(String expected) throws IOException {
                ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
                verify(mockClient, times(1)).execute(captor.capture());

                HttpUriRequest toVerify = captor.getValue();
                Header cookie = toVerify.getFirstHeader("cookie");
                Assertions.assertNotNull(cookie);
                Assertions.assertEquals(expected, cookie.getValue());
            }

            @Nested
            class WhenZosmfAuthenticateResponseLTPA {

                @BeforeEach
                void setZosmfResponse() {
                    Map<ZosmfService.TokenType, String> tokens = new HashMap<>();
                    String jwt = JWTUtils.createZoweJwtToken("user", "zosmf", "LTPA_token_from_zosmf", config);
                    tokens.put(ZosmfService.TokenType.JWT, jwt);
                    ZosmfService.AuthenticationResponse response = new ZosmfService.AuthenticationResponse("zosmf", tokens);
                    when(zosmfService.authenticate(any())).thenReturn(response);
                }

                @Test
                void thenOk() throws IOException {
                    applicationRegistry.setCurrentApplication(serviceWithCustomConfiguration.getId());

                    mockValid200HttpResponse();
                    given()
                        .config(SslContext.clientCertUser)
                        .when()
                        .get(basePath + serviceWithCustomConfiguration.getPath())
                        .then()
                        .statusCode(is(HttpStatus.SC_OK));
                    assertResult("LtpaToken2=LTPA_token_from_zosmf");

                }
            }

            @Nested
            class WhenZosmfAuthenticateResponseJWT {

                String zosmfJwtToken;

                @BeforeEach
                void setZosmfResponse() {
                    Map<ZosmfService.TokenType, String> tokens = new HashMap<>();
                    zosmfJwtToken = JWTUtils.createZosmfJwtToken("user", "zosmf", "LTPA_token_from_zosmf", config);
                    tokens.put(ZosmfService.TokenType.JWT, zosmfJwtToken);
                    ZosmfService.AuthenticationResponse response = new ZosmfService.AuthenticationResponse("zosmf", tokens);
                    when(zosmfService.authenticate(any())).thenReturn(response);
                }

                @Test
                void thenOk() throws IOException {

                    mockValid200HttpResponse();
                    given()
                        .config(SslContext.clientCertUser)
                        .when()
                        .get(basePath + serviceWithDefaultConfiguration.getPath())
                        .then()
                        .statusCode(is(HttpStatus.SC_OK));
                    assertResult("jwtToken=" + zosmfJwtToken);

                }
            }
        }

        /**
         * When client certificate from request does not have extended key usage set correctly and can't be used for
         * client authentication then request fails with response code 400 - BAD REQUEST
         */
        @Nested
        class WhenNoClientAuthInExtendedKeyUsage {
            void assertNoTransformation() throws IOException {
                ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
                verify(mockClient, times(1)).execute(captor.capture());

                HttpUriRequest toVerify = captor.getValue();
                Header cookie = toVerify.getFirstHeader("cookie");
                Assertions.assertNull(cookie);
            }

            @Test
            void whenNoClientAuthInExtendedKeyUsage_thenNoTransformation() throws IOException {
                mockValid200HttpResponse();
                given()
                    .config(SslContext.apimlRootCert)
                    .when()
                    .get(basePath + serviceWithDefaultConfiguration.getPath())
                    .then()
                    .statusCode(is(HttpStatus.SC_OK));
                assertNoTransformation();
            }

            @Test
            void whenServerCertificate_thenNoTransformation() throws IOException {
                mockValid200HttpResponse();
                given()
                    .config(SslContext.apimlRootCert)
                    .when()
                    .get(basePath + serviceWithDefaultConfiguration.getPath())
                    .then()
                    .statusCode(is(HttpStatus.SC_OK));
                assertNoTransformation();
            }
        }
    }
}
