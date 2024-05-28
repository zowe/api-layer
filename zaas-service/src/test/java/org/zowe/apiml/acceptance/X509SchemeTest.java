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
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithTwoServices;
import org.zowe.apiml.acceptance.netflix.MetadataBuilder;
import org.zowe.apiml.zaas.security.service.schema.X509Scheme;
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
class X509SchemeTest extends AcceptanceTestWithTwoServices {

    @Value("${server.ssl.keyStorePassword:password}")
    private char[] keystorePassword;
    @Value("${server.ssl.keyStore}")
    private String keystore;
    private final String clientKeystore = "../keystore/client_cert/client-certs.p12";


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

            validateHeaders("APIMTST", "CN=APIMTST, OU=Zowe, O=OMF, L=Prague, ST=Czechia, C=CZ", "MIID3jCCAsagAwIBAgIULApMeb1+40+ifLXNVf1mqwsNlt4wDQYJKoZIhvcNAQELBQAwYDELMAkGA1UEBhMCQ1oxEDAOBgNVBAgMB0N6ZWNoaWExDzANBgNVBAcMBlByYWd1ZTEMMAoGA1UECgwDT01GMQ0wCwYDVQQLDARab3dlMREwDwYDVQQDDAhBUElNTCBDQTAeFw0yMzA2MDIxMjQ4NDBaFw0yOTA1MzExMjQ4NDBaMF8xCzAJBgNVBAYTAkNaMRAwDgYDVQQIDAdDemVjaGlhMQ8wDQYDVQQHDAZQcmFndWUxDDAKBgNVBAoMA09NRjENMAsGA1UECwwEWm93ZTEQMA4GA1UEAwwHQVBJTVRTVDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJ6L+6l6mfxByy/VrHQ881xkW/GWQQndocPHi5Em15P+/ZQToYBTfLPUqGXcPnILg+PrjMtTHBCHO03pIuJxFXqrWfsaxR/O7zhpBSTt+iT6/kMBhPdF4sJF2VQo1sGBa79hIn3StvD3hKba/5Rzx8i+WXpKNeCzYRoZBLYH/MLAokgabf0iWjzrwy9STBvZ0uPON4iBhz6bYh0wTra90j0dDjsetTBMOrm9gO/sj7RD2KBQUM+mMiny5w4AWjvDChfzGEc37f/Ur2FyCqwY7k4oNS2tMtPQKemg4CtmFsWLL3Vb7e6fwoCNFLsmJumsd13u2HCmnV5YT13ZL8xphqkCAwEAAaOBkDCBjTALBgNVHQ8EBAMCBeAwHQYDVR0lBBYwFAYIKwYBBQUHAwIGCCsGAQUFBwMBMB8GA1UdEQQYMBaCCWxvY2FsaG9zdIIJMTI3LjAuMC4xMB0GA1UdDgQWBBQ3GrkUuyvHQmPRECqdzcR3qmQSHzAfBgNVHSMEGDAWgBT78hIus4SCXxMW8T9T0AEIe7HZNjANBgkqhkiG9w0BAQsFAAOCAQEAHAzeBownnYY9kSF6fif+dXw2miRTNkhRRc6ZIlijJy+d5ZysrR0yUTeW11raltGiX2gcCtg5GZp+ODgiqSMJN3mV1bIpKiuBhODKHlMzpg8v4ebjIHd1buO8KbOlR8zKv4kMFiGqdfWW6W3BZy3w3RCOnWhts2Y4O+XZ4GriYjiwkwf1IY7xv7HBJ4BsbUwxjxMcxa1HNqE8oAqEtiFxRmPkAi+g1lijvF26AKZdWxKFTLJV1HxUsa5l8b7cHN9yya6IVixVcB9Cla06Rg7dkaI4Deb5JCxFXjoznDKYkv8ZumkzQI9Ov90d1FYyVr7VWPEun/XV2XmH9nGHWyJSkA==");
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

            validateHeaders("Zowe Service", "CN=Zowe Service, OU=API Mediation Layer, O=Zowe Sample, L=Prague, ST=Prague, C=CZ", "MIIENzCCAx+gAwIBAgIEBUx4bjANBgkqhkiG9w0BAQsFADCBnjELMAkGA1UEBhMCQ1oxDzANBgNVBAgTBlByYWd1ZTEPMA0GA1UEBxMGUHJhZ3VlMRQwEgYDVQQKEwtab3dlIFNhbXBsZTEcMBoGA1UECxMTQVBJIE1lZGlhdGlvbiBMYXllcjE5MDcGA1UEAxMwWm93ZSBEZXZlbG9wbWVudCBJbnN0YW5jZXMgQ2VydGlmaWNhdGUgQXV0aG9yaXR5MB4XDTE5MDExMTEyMTIwN1oXDTI5MDEwODEyMTIwN1owejELMAkGA1UEBhMCQ1oxDzANBgNVBAgTBlByYWd1ZTEPMA0GA1UEBxMGUHJhZ3VlMRQwEgYDVQQKEwtab3dlIFNhbXBsZTEcMBoGA1UECxMTQVBJIE1lZGlhdGlvbiBMYXllcjEVMBMGA1UEAxMMWm93ZSBTZXJ2aWNlMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjo7rxDzO51tfSmqahMbY6lsXLO+/tXYk1ZcIufsh5L+UMs5StHlfSglbiRgWhfdJDTZb9R760klXL7QRYwBcYn3yhdYTsTB0+RJddPlTQzxAx45xV7b+fCtsQqBFZk5aes/TduyHCHXQRl+iLos13isrl5LSB66ohKxMtflPBeqTM/ptNBbq72XqFCQIZClClvMMYnxrW2FNfftxpLQbeFu3KN/8V4gcQoSUvE8YU8PYbVUnuhURActywrxHpke5q/tYQR8iDb6D1ZwLU8+/rTrnPbZq+O2DP7vRyBP9pHS/WNSxY1sTnz7gQ2OlUL+BEQLgRXRPc5ev1kwn0kVd8QIDAQABo4GfMIGcMB8GA1UdIwQYMBaAFPA6lVzMZhd6jkR4JClljOSWs0J1MB0GA1UdJQQWMBQGCCsGAQUFBwMCBggrBgEFBQcDATAOBgNVHQ8BAf8EBAMCBPAwKwYDVR0RBCQwIoIVbG9jYWxob3N0LmxvY2FsZG9tYWlugglsb2NhbGhvc3QwHQYDVR0OBBYEFJDw32hIl2AHqtLlFJtyVkrIlaGjMA0GCSqGSIb3DQEBCwUAA4IBAQAwO1TPIg5ebOiotTtJgj2wbyYFBfqljLrBMEfgP6h6ZOkj5fQIdZSLNmyY/PSk8IHUPE43QzEPV8Bd2zOwtDzbrnfvtuKLuLzPr+shih3gpUoSYGLU2miZZerk4AhpOrjIaUvKgcZ5QU7EQy32kQuKf9ldozxgnOzgN60G5z/qae7fYZxoSeV/nq8t7AkognCwHAKx8Iy418ucsfAuXQbursVWMi3KHrSENimZ+3fgCJ3ym0QTqwTpojppW5F9SWkJ4Q31l+oRROwIRKm44XSB8DVFnX/k8gzTPMylfQ+GwEyVHcyAR9zBnNhbbueFLlG9CBMeCHCyia6DUdIQlY5/");
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
