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
import org.zowe.apiml.gateway.security.service.schema.X509Scheme;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.SslContextConfigurer;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
    class GivenValidClientCertificate {

        @Test
        void thenCertDetailsInRequestHeaders() throws IOException {
            applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
            mockValid200HttpResponse();

            given()
                .config(SslContext.clientCertValid)
                .when()
                .get(basePath + serviceWithDefaultConfiguration.getPath())
                .then()
                .statusCode(is(HttpStatus.SC_OK));

            ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
            verify(mockClient, times(1)).execute(captor.capture());

            HttpUriRequest toVerify = captor.getValue();
            Header cnHeader = toVerify.getFirstHeader(X509Scheme.X509Command.COMMON_NAME);
            Header dnHeader = toVerify.getFirstHeader(X509Scheme.X509Command.DISTINGUISHED_NAME);
            Header publicKeyHeader = toVerify.getFirstHeader(X509Scheme.X509Command.PUBLIC_KEY);

            Assertions.assertNotNull(cnHeader);
            Assertions.assertEquals("APIMTST", cnHeader.getValue());
            Assertions.assertNotNull(dnHeader);
            Assertions.assertEquals("CN=APIMTST, OU=CA CZ, O=Broadcom, L=Prague, ST=Czechia, C=CZ", dnHeader.getValue());
            Assertions.assertNotNull(publicKeyHeader);
            Assertions.assertEquals("MIID9DCCAtygAwIBAgIUVyBCWfHF/ZwZKVsBEpTNIBj9mQkwDQYJKoZIhvcNAQELBQAwfzELMAkGA1UEBhMCQ1oxDzANBgNVBAgMBlByYWd1ZTEPMA0GA1UEBwwGUHJhZ3VlMREwDwYDVQQKDAhCcm9hZGNvbTEMMAoGA1UECwwDTUZEMS0wKwYDVQQDDCRBUElNTCBFeHRlcm5hbCBDZXJ0aWZpY2F0ZSBBdXRob3JpdHkwHhcNMjAwOTA0MTE0NTEwWhcNMjMwNjAxMTE0NTEwWjBlMQswCQYDVQQGEwJDWjEQMA4GA1UECAwHQ3plY2hpYTEPMA0GA1UEBwwGUHJhZ3VlMREwDwYDVQQKDAhCcm9hZGNvbTEOMAwGA1UECwwFQ0EgQ1oxEDAOBgNVBAMMB0FQSU1UU1QwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCVFRHCchIOADnsFoEfJJ9olPVE2Lu7sEEd5JLmOvzs14Hg4zCNNfBAUMgOsgsjJXMYMugJaPuswMUNjed3ankwmndqYGftzhl4apTpVOHoWEj+mYxXj00LcX/76yr5aVz7e2pWHYSZDbov++hzO0yX5yqPAloJMD+YFWtjWuS2DjQe2GuB669Mjjy57f4swlcrdhxOUjyN3yv8dfhxIdEZsCZ+4RewVkcXa19uu+T7MBE2RF2FbQ17Jj8oIie1eapO20/LtulTX/x7vov2bdOYtAHtATmG52Opo1ttzHushjr7+Oy+gsSfzTVKet4IzC6EiL9I26HbblcDWKMfeLVVAgMBAAGjgYEwfzALBgNVHQ8EBAMCBeAwHQYDVR0lBBYwFAYIKwYBBQUHAwIGCCsGAQUFBwMBMB0GA1UdDgQWBBQKnjZ80tko0IA8jz3klXH75isM5DARBglghkgBhvhCAQEEBAMCBeAwHwYDVR0jBBgwFoAUcYHv14ClCeqgaHg5n4LYjlmgj3cwDQYJKoZIhvcNAQELBQADggEBAB5PJqrHxduzsf39O3zHyM+o9RfuNeMefmCK0GwuumZD5tCO3L3/QatK07HAW/Koa0mh+DbTsf9syN2GG+ZMNtvRNr9kAVfLOVImuME3WzyQFRxWaHFMRn2UkFBle8JMwDmXxzFE7INMsz54whGBQpGFBCTEIlbV8QBu7tyz/glnuu7pKc084WdnsfuQUR2bjRU8DgfaOEQI5vK4IZv6wUYW2qj42mcT6ykK3AZ8fSYj9Zg3NVocu4jslFUYo7C5vHk99bgyYg/jlZ5mmeJVrnRWL4N/RpUfhcgGtscYwRnNWpy3tcadutRdKS3JI/gizx62PohAfxL3E+u+73Olxf0=", publicKeyHeader.getValue());
        }
    }

    // Currently, internal APIML certificate will be passed through to a service. This should not be the case in the future.
    @Nested
    class GivenInternalApimlCertificate {

        @Test
        void thenCertDetailsInRequestHeaders() throws IOException {
            applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
            mockValid200HttpResponse();

            given()
                .config(SslContext.clientCertApiml)
                .when()
                .get(basePath + serviceWithDefaultConfiguration.getPath())
                .then()
                .statusCode(is(HttpStatus.SC_OK));

            ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
            verify(mockClient, times(1)).execute(captor.capture());

            HttpUriRequest toVerify = captor.getValue();
            Header cnHeader = toVerify.getFirstHeader(X509Scheme.X509Command.COMMON_NAME);
            Header dnHeader = toVerify.getFirstHeader(X509Scheme.X509Command.DISTINGUISHED_NAME);
            Header publicKeyHeader = toVerify.getFirstHeader(X509Scheme.X509Command.PUBLIC_KEY);

            Assertions.assertNotNull(cnHeader);
            Assertions.assertEquals("Zowe Service", cnHeader.getValue());
            Assertions.assertNotNull(dnHeader);
            Assertions.assertEquals("CN=Zowe Service, OU=API Mediation Layer, O=Zowe Sample, L=Prague, ST=Prague, C=CZ", dnHeader.getValue());
            Assertions.assertNotNull(publicKeyHeader);
            Assertions.assertEquals("MIIENzCCAx+gAwIBAgIEBUx4bjANBgkqhkiG9w0BAQsFADCBnjELMAkGA1UEBhMCQ1oxDzANBgNVBAgTBlByYWd1ZTEPMA0GA1UEBxMGUHJhZ3VlMRQwEgYDVQQKEwtab3dlIFNhbXBsZTEcMBoGA1UECxMTQVBJIE1lZGlhdGlvbiBMYXllcjE5MDcGA1UEAxMwWm93ZSBEZXZlbG9wbWVudCBJbnN0YW5jZXMgQ2VydGlmaWNhdGUgQXV0aG9yaXR5MB4XDTE5MDExMTEyMTIwN1oXDTI5MDEwODEyMTIwN1owejELMAkGA1UEBhMCQ1oxDzANBgNVBAgTBlByYWd1ZTEPMA0GA1UEBxMGUHJhZ3VlMRQwEgYDVQQKEwtab3dlIFNhbXBsZTEcMBoGA1UECxMTQVBJIE1lZGlhdGlvbiBMYXllcjEVMBMGA1UEAxMMWm93ZSBTZXJ2aWNlMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjo7rxDzO51tfSmqahMbY6lsXLO+/tXYk1ZcIufsh5L+UMs5StHlfSglbiRgWhfdJDTZb9R760klXL7QRYwBcYn3yhdYTsTB0+RJddPlTQzxAx45xV7b+fCtsQqBFZk5aes/TduyHCHXQRl+iLos13isrl5LSB66ohKxMtflPBeqTM/ptNBbq72XqFCQIZClClvMMYnxrW2FNfftxpLQbeFu3KN/8V4gcQoSUvE8YU8PYbVUnuhURActywrxHpke5q/tYQR8iDb6D1ZwLU8+/rTrnPbZq+O2DP7vRyBP9pHS/WNSxY1sTnz7gQ2OlUL+BEQLgRXRPc5ev1kwn0kVd8QIDAQABo4GfMIGcMB8GA1UdIwQYMBaAFPA6lVzMZhd6jkR4JClljOSWs0J1MB0GA1UdJQQWMBQGCCsGAQUFBwMCBggrBgEFBQcDATAOBgNVHQ8BAf8EBAMCBPAwKwYDVR0RBCQwIoIVbG9jYWxob3N0LmxvY2FsZG9tYWlugglsb2NhbGhvc3QwHQYDVR0OBBYEFJDw32hIl2AHqtLlFJtyVkrIlaGjMA0GCSqGSIb3DQEBCwUAA4IBAQAwO1TPIg5ebOiotTtJgj2wbyYFBfqljLrBMEfgP6h6ZOkj5fQIdZSLNmyY/PSk8IHUPE43QzEPV8Bd2zOwtDzbrnfvtuKLuLzPr+shih3gpUoSYGLU2miZZerk4AhpOrjIaUvKgcZ5QU7EQy32kQuKf9ldozxgnOzgN60G5z/qae7fYZxoSeV/nq8t7AkognCwHAKx8Iy418ucsfAuXQbursVWMi3KHrSENimZ+3fgCJ3ym0QTqwTpojppW5F9SWkJ4Q31l+oRROwIRKm44XSB8DVFnX/k8gzTPMylfQ+GwEyVHcyAR9zBnNhbbueFLlG9CBMeCHCyia6DUdIQlY5/", publicKeyHeader.getValue());

        }
    }
}
