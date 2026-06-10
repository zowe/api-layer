/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.error;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceAccessClassifierTest {

    @Nested
    class IsServiceUnavailable {

        @Test
        void givenNull_whenIsServiceUnavailable_thenReturnsFalse() {
            assertFalse(ServiceAccessClassifier.isServiceUnavailable(null));
        }

        // --- Phase 1: SSL/cert exceptions → false ---

        @Test
        void givenSslExceptionDirect_whenIsServiceUnavailable_thenReturnsFalse() {
            SSLException ex = new SSLException("TLS handshake failed");
            assertFalse(ServiceAccessClassifier.isServiceUnavailable(ex));
        }

        @Test
        void givenCertificateExceptionDirect_whenIsServiceUnavailable_thenReturnsFalse() {
            CertificateException ex = new CertificateException("cert expired");
            assertFalse(ServiceAccessClassifier.isServiceUnavailable(ex));
        }

        @Test
        void givenSslWrappedInConnect_whenIsServiceUnavailable_thenReturnsFalse() {
            SSLException sslEx = new SSLException("TLS handshake failed");
            ConnectException connectEx = new ConnectException("connection refused");
            connectEx.initCause(sslEx);
            assertFalse(ServiceAccessClassifier.isServiceUnavailable(connectEx));
        }

        // --- Phase 2: connectivity exceptions → true ---

        @Test
        void givenConnectExceptionDirect_whenIsServiceUnavailable_thenReturnsTrue() {
            ConnectException ex = new ConnectException("connection refused");
            assertTrue(ServiceAccessClassifier.isServiceUnavailable(ex));
        }

        @Test
        void givenSocketTimeoutExceptionDirect_whenIsServiceUnavailable_thenReturnsTrue() {
            SocketTimeoutException ex = new SocketTimeoutException("read timed out");
            assertTrue(ServiceAccessClassifier.isServiceUnavailable(ex));
        }

        @Test
        void givenSocketExceptionDirect_whenIsServiceUnavailable_thenReturnsTrue() {
            SocketException ex = new SocketException("connection reset");
            assertTrue(ServiceAccessClassifier.isServiceUnavailable(ex));
        }

        @Test
        void givenNoRouteToHostExceptionDirect_whenIsServiceUnavailable_thenReturnsTrue() {
            NoRouteToHostException ex = new NoRouteToHostException("no route to host");
            assertTrue(ServiceAccessClassifier.isServiceUnavailable(ex));
        }

        @Test
        void givenUnknownHostExceptionDirect_whenIsServiceUnavailable_thenReturnsTrue() {
            UnknownHostException ex = new UnknownHostException("unknown host");
            assertTrue(ServiceAccessClassifier.isServiceUnavailable(ex));
        }

        @Test
        void givenConnectWrappedInRuntime_whenIsServiceUnavailable_thenReturnsTrue() {
            ConnectException connectEx = new ConnectException("connection refused");
            RuntimeException runtimeEx = new RuntimeException("wrapper", connectEx);
            assertTrue(ServiceAccessClassifier.isServiceUnavailable(runtimeEx));
        }

        @Test
        void givenDeepChainWithConnect_whenIsServiceUnavailable_thenReturnsTrue() {
            ConnectException connectEx = new ConnectException("connection refused");
            IOException ioEx = new IOException("wrapper B", connectEx);
            RuntimeException runtimeEx = new RuntimeException("wrapper A", ioEx);
            assertTrue(ServiceAccessClassifier.isServiceUnavailable(runtimeEx));
        }

        @Test
        void givenNonConnectIOException_whenIsServiceUnavailable_thenReturnsFalse() {
            IOException ex = new IOException("generic I/O error");
            assertFalse(ServiceAccessClassifier.isServiceUnavailable(ex));
        }
    }

    @Nested
    class HasSslOrCertificateCause {

        @Test
        void givenNull_whenHasSslOrCertificateCause_thenReturnsFalse() {
            assertFalse(ServiceAccessClassifier.hasSslOrCertificateCause(null));
        }

        @Test
        void givenSslExceptionDirect_whenHasSslOrCertificateCause_thenReturnsTrue() {
            SSLException ex = new SSLException("TLS handshake failed");
            assertTrue(ServiceAccessClassifier.hasSslOrCertificateCause(ex));
        }

        @Test
        void givenCertificateExceptionDirect_whenHasSslOrCertificateCause_thenReturnsTrue() {
            CertificateException ex = new CertificateException("cert expired");
            assertTrue(ServiceAccessClassifier.hasSslOrCertificateCause(ex));
        }

        @Test
        void givenConnectWrappedAroundSsl_whenHasSslOrCertificateCause_thenReturnsTrue() {
            SSLException sslEx = new SSLException("TLS handshake failed");
            ConnectException connectEx = new ConnectException("connection refused");
            connectEx.initCause(sslEx);
            assertTrue(ServiceAccessClassifier.hasSslOrCertificateCause(connectEx));
        }

        @Test
        void givenConnectWithoutSsl_whenHasSslOrCertificateCause_thenReturnsFalse() {
            ConnectException ex = new ConnectException("connection refused");
            assertFalse(ServiceAccessClassifier.hasSslOrCertificateCause(ex));
        }

        @Test
        void givenDeepChainWithCertificate_whenHasSslOrCertificateCause_thenReturnsTrue() {
            CertificateException certEx = new CertificateException("cert expired");
            SSLException sslEx = new SSLException("wrapping", certEx);
            RuntimeException runtimeEx = new RuntimeException("top", sslEx);
            assertTrue(ServiceAccessClassifier.hasSslOrCertificateCause(runtimeEx));
        }
    }
}
