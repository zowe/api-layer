/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.service;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.gateway.config.ConnectionsConfig;
import org.zowe.apiml.security.HttpsConfigError;
import org.zowe.apiml.security.SecurityUtils;

import java.io.ByteArrayInputStream;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CertificateChainServiceTest {
    private CertificateChainService certificateChainService;
    ConnectionsConfig connectionsConfig = new ConnectionsConfig(null);

    private static final String CERTIFICATE_1 =
        """
            -----BEGIN CERTIFICATE-----
            MIIENzCCAx+gAwIBAgIEBUx4bjANBgkqhkiG9w0BAQsFADCBnjELMAkGA1UEBhMC
            Q1oxDzANBgNVBAgTBlByYWd1ZTEPMA0GA1UEBxMGUHJhZ3VlMRQwEgYDVQQKEwta
            b3dlIFNhbXBsZTEcMBoGA1UECxMTQVBJIE1lZGlhdGlvbiBMYXllcjE5MDcGA1UE
            AxMwWm93ZSBEZXZlbG9wbWVudCBJbnN0YW5jZXMgQ2VydGlmaWNhdGUgQXV0aG9y
            aXR5MB4XDTE5MDExMTEyMTIwN1oXDTI5MDEwODEyMTIwN1owejELMAkGA1UEBhMC
            Q1oxDzANBgNVBAgTBlByYWd1ZTEPMA0GA1UEBxMGUHJhZ3VlMRQwEgYDVQQKEwta
            b3dlIFNhbXBsZTEcMBoGA1UECxMTQVBJIE1lZGlhdGlvbiBMYXllcjEVMBMGA1UE
            AxMMWm93ZSBTZXJ2aWNlMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA
            jo7rxDzO51tfSmqahMbY6lsXLO+/tXYk1ZcIufsh5L+UMs5StHlfSglbiRgWhfdJ
            DTZb9R760klXL7QRYwBcYn3yhdYTsTB0+RJddPlTQzxAx45xV7b+fCtsQqBFZk5a
            es/TduyHCHXQRl+iLos13isrl5LSB66ohKxMtflPBeqTM/ptNBbq72XqFCQIZClC
            lvMMYnxrW2FNfftxpLQbeFu3KN/8V4gcQoSUvE8YU8PYbVUnuhURActywrxHpke5
            q/tYQR8iDb6D1ZwLU8+/rTrnPbZq+O2DP7vRyBP9pHS/WNSxY1sTnz7gQ2OlUL+B
            EQLgRXRPc5ev1kwn0kVd8QIDAQABo4GfMIGcMB8GA1UdIwQYMBaAFPA6lVzMZhd6
            jkR4JClljOSWs0J1MB0GA1UdJQQWMBQGCCsGAQUFBwMCBggrBgEFBQcDATAOBgNV
            HQ8BAf8EBAMCBPAwKwYDVR0RBCQwIoIVbG9jYWxob3N0LmxvY2FsZG9tYWluggls
            b2NhbGhvc3QwHQYDVR0OBBYEFJDw32hIl2AHqtLlFJtyVkrIlaGjMA0GCSqGSIb3
            DQEBCwUAA4IBAQAwO1TPIg5ebOiotTtJgj2wbyYFBfqljLrBMEfgP6h6ZOkj5fQI
            dZSLNmyY/PSk8IHUPE43QzEPV8Bd2zOwtDzbrnfvtuKLuLzPr+shih3gpUoSYGLU
            2miZZerk4AhpOrjIaUvKgcZ5QU7EQy32kQuKf9ldozxgnOzgN60G5z/qae7fYZxo
            SeV/nq8t7AkognCwHAKx8Iy418ucsfAuXQbursVWMi3KHrSENimZ+3fgCJ3ym0QT
            qwTpojppW5F9SWkJ4Q31l+oRROwIRKm44XSB8DVFnX/k8gzTPMylfQ+GwEyVHcyA
            R9zBnNhbbueFLlG9CBMeCHCyia6DUdIQlY5/
            -----END CERTIFICATE-----
            """.stripIndent();

    private static final String CERTIFICATE_2 =
        """
            -----BEGIN CERTIFICATE-----
            MIID+zCCAuOgAwIBAgIEdkRICDANBgkqhkiG9w0BAQsFADCBnjELMAkGA1UEBhMC
            Q1oxDzANBgNVBAgTBlByYWd1ZTEPMA0GA1UEBxMGUHJhZ3VlMRQwEgYDVQQKEwta
            b3dlIFNhbXBsZTEcMBoGA1UECxMTQVBJIE1lZGlhdGlvbiBMYXllcjE5MDcGA1UE
            AxMwWm93ZSBEZXZlbG9wbWVudCBJbnN0YW5jZXMgQ2VydGlmaWNhdGUgQXV0aG9y
            aXR5MB4XDTE5MDExMTEyMTIwNVoXDTI5MDEwODEyMTIwNVowgZ4xCzAJBgNVBAYT
            AkNaMQ8wDQYDVQQIEwZQcmFndWUxDzANBgNVBAcTBlByYWd1ZTEUMBIGA1UEChML
            Wm93ZSBTYW1wbGUxHDAaBgNVBAsTE0FQSSBNZWRpYXRpb24gTGF5ZXIxOTA3BgNV
            BAMTMFpvd2UgRGV2ZWxvcG1lbnQgSW5zdGFuY2VzIENlcnRpZmljYXRlIEF1dGhv
            cml0eTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALyotswfS+sLTmwO
            08ocbkNWPccRVWGWaP/LvfLe1USmhUOMO7E38ztTy8AJYBxrFTPr2lL3rXybRHCn
            Lscz0XNvkNll6Yef71ghaLbpe0V12Jygw4J9BAbYdVIsrP+brR3pijGVO/ECvJwD
            815ODsGU3Staw9HFlHO7dWss/TM2uz3Y6oVLObuhEWvAXiU3fW3PpFebRUlhLe5g
            yprGSZDFQAJpiqD7Nac5uZB53ETSPI+Cyku2E5CPx3qDJh9ueiHyaqmtbhBXjOue
            7rHU9F03zpldofqp4WDMnrl9ktzQDx+OHY5HI+gsaKV/MEX3YVrD+Rdc1GTc0JiI
            OS2VBCsCAwEAAaM/MD0wDwYDVR0TAQH/BAUwAwEB/zALBgNVHQ8EBAMCAgQwHQYD
            VR0OBBYEFPA6lVzMZhd6jkR4JClljOSWs0J1MA0GCSqGSIb3DQEBCwUAA4IBAQCt
            onZY1WkhTXmBxIl6EW/IDmcXZeYgucw590I7iVVXDi53oCM16AIM6pniqMP/iku5
            2MX2JqGD//eEnJDt6q+qA4htJSb7lswjbC90xLkGAKAuDsC2cKGaoQAeTh5ouP7C
            itN2+xVjZTfyAg3ZxmhXmVKVsv4rRpiAOYvX7R7ewNjpJkBeTQouind5rKtabzPD
            0nHKF0u/Y8FaEwv8zFRffsnl0/3nqfnT6l0mvekDP+LhIKZI9TwIJYkP9PGraR50
            HgUnKdoaJuPVQfbiMzISRqXygfTdmVnY9CEP9/W2S4NgaLXI6AkNLEcLNvz/CKJg
            TRqXQKkvunUCrHxi5oV+
            -----END CERTIFICATE-----
            """;

    private Certificate[] certificates = new Certificate[2];

    @Nested
    class GivenValidCertificateChain {

        @BeforeEach
        void setup() throws CertificateException {
            certificates[0] = generateCert(CERTIFICATE_1);
            certificates[1] = generateCert(CERTIFICATE_2);
            certificateChainService = new CertificateChainService(connectionsConfig);
            ReflectionTestUtils.setField(certificateChainService, "certificates", certificates, Certificate[].class);
        }

        private String normalizeText(String input) {
            return StringUtils.replace(input, "\r\n", "\n");
        }

        @Test
        void whenGetCertificates_thenPEMIsProduced() {
            String result = normalizeText(certificateChainService.getCertificatesInPEMFormat());
            assertNotNull(result);
            assertEquals(normalizeText(CERTIFICATE_1 + CERTIFICATE_2), result);
        }
    }

    @Nested
    class GivenNoCertificatesInChain {
        @BeforeEach
        void setup() {
            certificateChainService = new CertificateChainService(connectionsConfig);
            ReflectionTestUtils.setField(certificateChainService, "certificates", new Certificate[0], Certificate[].class);
        }

        @Test
        void whenGetCertificates_thenEmptyStringReturned() {
            String result = certificateChainService.getCertificatesInPEMFormat();
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class GivenInvalidCertificateInChain {
        @BeforeEach
        void setup() throws CertificateException {
            certificates[0] = generateCert(CERTIFICATE_1);
            certificates[1] = mock(Certificate.class);
            when(certificates[1].getEncoded()).thenReturn("INVALID_CERT_CONTENT".getBytes());
            certificateChainService = new CertificateChainService(connectionsConfig);
            ReflectionTestUtils.setField(certificateChainService, "certificates", certificates, Certificate[].class);
        }

        @Test
        void whenGetCertificates_thenNullReturned() {
            String result = certificateChainService.getCertificatesInPEMFormat();
            assertNull(result);
        }
    }

    @Nested
    class GivenExceptionDuringChainLoad {

        @BeforeEach
        void setup() {
            certificateChainService = new CertificateChainService(connectionsConfig);
        }

        @Test
        void whenConstructService_thenExceptionThrown() {
            try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
                securityUtilsMockedStatic.when(() -> SecurityUtils.loadCertificateChain(any()))
                    .thenThrow(new KeyStoreException("invalid keystore"));
                Exception thrown = assertThrows(HttpsConfigError.class, () -> certificateChainService.loadCertChain());
                assertEquals("Error initializing SSL Context: invalid keystore", thrown.getMessage());
            }
        }
    }

    private Certificate generateCert(@NonNull String pem) throws CertificateException {
        ByteArrayInputStream in = new ByteArrayInputStream(pem.getBytes());
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return cf.generateCertificate(in);
    }
}
