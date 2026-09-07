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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithTwoServices;
import org.zowe.apiml.acceptance.netflix.MetadataBuilder;
import org.zowe.apiml.gateway.security.service.schema.X509Scheme;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.SslContextConfigurer;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.constants.ApimlConstants.AUTH_FAIL_HEADER;

/**
 * This test verifies that only the client certificate is passed through the X509scheme to the southbound service.
 * <p></p>
 * Note: 2022/02/28 - current implementation of {@link X509Scheme} works with any certificate located in either default
 * javax.servlet.request.X509Certificate attribute or in client.auth.X509Certificate custom attribute introduced
 * by the {@link org.zowe.apiml.security.common.filter.CategorizeCertsFilter CategorizeCertsFilter}.
 */
@AcceptanceTest
@ActiveProfiles("X509SchemeTest")
class X509SchemeTest extends AcceptanceTestWithTwoServices {

    @Value("${server.ssl.keyStorePassword:password}")
    private char[] keystorePassword;
    @Value("${server.ssl.keyStore}")
    private String keystore;
    private final String clientKeystore = "../keystore/client/client-certs.p12";


    @BeforeEach
    void setUp() throws Exception {
        SslContextConfigurer configurer = new SslContextConfigurer(keystorePassword, clientKeystore, keystore);
        SslContext.prepareSslAuthentication(configurer);

        applicationRegistry.clearApplications();
        MetadataBuilder customBuilder = MetadataBuilder.customInstance();
        customBuilder.withX509();
        MetadataBuilder defaultBuilder = MetadataBuilder.defaultInstance();
        defaultBuilder.withX509();
        applicationRegistry.addApplication(serviceWithDefaultConfiguration, defaultBuilder, false);
        applicationRegistry.addApplication(serviceWithCustomConfiguration, customBuilder, true);

        reset(mockClient);
    }

    @Nested
    class GivenValidCertificate {

        @Test
        void whenClientCertificate_thenCertDetailsInRequestHeaders() throws IOException {
            applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
            mockValid200HttpResponse();

            given()
                .config(SslContext.clientCertValid)
                .when()
                .get(basePath + serviceWithDefaultConfiguration.getPath())
                .then()
                .statusCode(is(HttpStatus.SC_OK));

            validateHeaders("APIMTST", "CN=APIMTST, OU=Zowe, O=OMF, L=Prague, ST=Czechia, C=CZ", SslContext.clientCertValidCert);
        }

        @Test
        void whenInternalApimlCertificate_thenCertDetailsInRequestHeaders() throws IOException {
            applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
            mockValid200HttpResponse();

            given()
                .config(SslContext.clientCertApiml)
                .when()
                .get(basePath + serviceWithDefaultConfiguration.getPath())
                .then()
                .statusCode(is(HttpStatus.SC_OK));

            validateHeaders("Zowe Service", "CN=Zowe Service, OU=API Mediation Layer, O=Zowe Sample, L=Prague, ST=Prague, C=CZ", SslContext.apimlServiceCertBase64);
        }

        private void validateHeaders(String expectedCnHeader, String expectedDnHeader, String expectedPublicKeyHeader) throws IOException {
            ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
            verify(mockClient, times(1)).execute(captor.capture());

            assertHeaders(captor.getValue(), expectedCnHeader, expectedDnHeader, expectedPublicKeyHeader);
        }

        private void assertHeaders(HttpUriRequest toVerify, String expectedCnHeader, String expectedDnHeader, String expectedPublicKeyHeader) {
            Header cnHeader = toVerify.getFirstHeader(X509Scheme.X509Command.COMMON_NAME);
            Header dnHeader = toVerify.getFirstHeader(X509Scheme.X509Command.DISTINGUISHED_NAME);
            Header publicKeyHeader = toVerify.getFirstHeader(X509Scheme.X509Command.PUBLIC_KEY);

            Assertions.assertNotNull(cnHeader);
            Assertions.assertEquals(expectedCnHeader, cnHeader.getValue());
            Assertions.assertNotNull(dnHeader);
            Assertions.assertEquals(expectedDnHeader, dnHeader.getValue());
            Assertions.assertNotNull(publicKeyHeader);
            Assertions.assertEquals(expectedPublicKeyHeader, publicKeyHeader.getValue());
        }
    }

    @Nested
    class GivenInvalidCertificate {

        @Test
        void whenNoCertificate_thenNoCertDetailsInRequestHeaders() throws IOException {
            String errorHeaderValue = "ZWEAG167E No client certificate provided in the request";

            applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
            mockValid200HttpResponse();

            given()
                .when()
                .get(basePath + serviceWithDefaultConfiguration.getPath())
                .then()
                .statusCode(is(HttpStatus.SC_OK));

            ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
            verify(mockClient, times(1)).execute(captor.capture());

            assertHeaders(captor.getValue(), errorHeaderValue);
        }

        private void assertHeaders(HttpUriRequest toVerify, String errorMessage) throws IOException {
            ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
            verify(mockClient, times(1)).execute(captor.capture());

            Header xZoweAuthFailureHeader = toVerify.getFirstHeader(AUTH_FAIL_HEADER);
            Assertions.assertNotNull(xZoweAuthFailureHeader);
            Assertions.assertEquals(errorMessage, xZoweAuthFailureHeader.getValue());
            assertThat(captor.getValue().getHeaders(X509Scheme.X509Command.COMMON_NAME).length, is(0));
            assertThat(captor.getValue().getHeaders(X509Scheme.X509Command.DISTINGUISHED_NAME).length, is(0));
            assertThat(captor.getValue().getHeaders(X509Scheme.X509Command.PUBLIC_KEY).length, is(0));
        }
    }
}
