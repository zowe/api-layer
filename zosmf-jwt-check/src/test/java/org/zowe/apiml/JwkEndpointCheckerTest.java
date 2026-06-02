/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.net.ssl.SSLHandshakeException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwkEndpointCheckerTest {

    private final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    private HttpClientWrapper mockClient;
    private ZosmfJwtCheckConfig mockConf;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outStream));
        System.setErr(new PrintStream(errStream));

        mockClient = mock(HttpClientWrapper.class);
        mockConf = mock(ZosmfJwtCheckConfig.class);
        when(mockConf.getScheme()).thenReturn("https");
        when(mockConf.getZosmfHost()).thenReturn("zosmf.example.com");
        when(mockConf.getZosmfPort()).thenReturn(443);
        when(mockConf.isVerbose()).thenReturn(false);
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Nested
    class SuccessResponses {

        @Test
        void response200IsSuccess() throws IOException {
            when(mockClient.executeCall(any(), anyMap())).thenReturn(new HttpClientWrapper.Response(200, "{\"keys\":[{\"kty\":\"RSA\",\"e\":\"AQAB\",\"n\":\"validModulusValue\"}]}"));
            JwkEndpointChecker checker = new JwkEndpointChecker(mockClient, mockConf);
            assertTrue(checker.check());
            assertTrue(outStream.toString().contains("SUCCESS"));
            assertTrue(outStream.toString().contains("200"));
        }

        @Test
        void response401IsSuccess() throws IOException {
            when(mockClient.executeCall(any(), anyMap())).thenReturn(new HttpClientWrapper.Response(401, ""));
            JwkEndpointChecker checker = new JwkEndpointChecker(mockClient, mockConf);
            assertTrue(checker.check());
            assertTrue(outStream.toString().contains("SUCCESS"));
            assertTrue(outStream.toString().contains("401"));
        }

        @Test
        void response200WithEmptyModulusIsFailure() throws IOException {
            when(mockClient.executeCall(any(), anyMap())).thenReturn(new HttpClientWrapper.Response(200, "{\"keys\":[{\"kty\":\"RSA\",\"e\":\"AQAB\",\"n\":\"\"}]}"));
            JwkEndpointChecker checker = new JwkEndpointChecker(mockClient, mockConf);
            assertFalse(checker.check());
            assertTrue(errStream.toString().contains("empty RSA modulus"));
        }
    }

    @Nested
    class FailureResponses {

        @ParameterizedTest
        @CsvSource({
            "404, 404",
            "500, server error",
            "403, client error"
        })
        void failureResponseCodesAreReported(int statusCode, String expectedMessage) throws IOException {
            when(mockClient.executeCall(any(), anyMap())).thenReturn(new HttpClientWrapper.Response(statusCode, ""));
            JwkEndpointChecker checker = new JwkEndpointChecker(mockClient, mockConf);
            assertFalse(checker.check());
            assertTrue(errStream.toString().contains("FAILURE"));
            assertTrue(errStream.toString().contains(expectedMessage));
        }
    }

    @Nested
    class ExceptionHandling {

        @Test
        void sslHandshakeExceptionReportsCertificateError() throws IOException {
            when(mockClient.executeCall(any(), anyMap())).thenThrow(new SSLHandshakeException("certificate unknown"));
            JwkEndpointChecker checker = new JwkEndpointChecker(mockClient, mockConf);
            assertFalse(checker.check());
            assertTrue(errStream.toString().contains("SSL handshake failed"));
            assertTrue(errStream.toString().contains("truststore"));
        }

        @Test
        void connectExceptionReportsUnreachable() throws IOException {
            when(mockClient.executeCall(any(), anyMap())).thenThrow(new ConnectException("Connection refused"));
            JwkEndpointChecker checker = new JwkEndpointChecker(mockClient, mockConf);
            assertFalse(checker.check());
            assertTrue(errStream.toString().contains("Cannot connect"));
        }

        @Test
        void socketTimeoutExceptionReportsTimeout() throws IOException {
            when(mockClient.executeCall(any(), anyMap())).thenThrow(new SocketTimeoutException("Read timed out"));
            JwkEndpointChecker checker = new JwkEndpointChecker(mockClient, mockConf);
            assertFalse(checker.check());
            assertTrue(errStream.toString().contains("timed out"));
        }

        @Test
        void unexpectedExceptionIsHandled() throws IOException {
            when(mockClient.executeCall(any(), anyMap())).thenThrow(new IOException("unexpected"));
            JwkEndpointChecker checker = new JwkEndpointChecker(mockClient, mockConf);
            assertFalse(checker.check());
            assertTrue(errStream.toString().contains("verify hostname and port"));
        }
    }
}
