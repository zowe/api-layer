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

import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;

/**
 * Utility that inspects exception cause chains to classify service access failures.
 *
 * <p>The classifier distinguishes between SSL/TLS certificate errors (which indicate a
 * configuration problem, not a down service) and genuine connectivity failures (service
 * is unreachable).</p>
 *
 * <p>Two-phase cause-chain inspection for {@link #isServiceUnavailable(Throwable)}:
 * <ol>
 *   <li>Scan for SSL/cert exceptions (negative signal — returns false if found)</li>
 *   <li>Scan for connectivity exceptions (positive signal — returns true if found)</li>
 * </ol></p>
 */
public final class ServiceAccessClassifier {

    private ServiceAccessClassifier() {
    }

    /**
     * Checks whether the exception indicates the target service is genuinely unreachable.
     *
     * <p>Two-phase inspection of the full cause chain:</p>
     * <ol>
     *   <li>If ANY link is an SSLException or CertificateException, this is a TLS/cert
     *       configuration problem, not a down service — returns {@code false}.</li>
     *   <li>Otherwise, walks the chain looking for connectivity exceptions
     *       (ConnectException, SocketTimeoutException, SocketException,
     *       NoRouteToHostException, UnknownHostException) and returns {@code true}
     *       if any is found.</li>
     * </ol>
     *
     * @param error the exception to inspect; may be {@code null}
     * @return {@code true} if the error chain indicates genuine service unavailability,
     *         {@code false} for SSL/cert errors, non-connectivity errors, or {@code null}
     */
    public static boolean isServiceUnavailable(Throwable error) {
        if (error == null) {
            return false;
        }

        // Phase 1: check for SSL/certificate exceptions anywhere in the chain
        if (hasSslOrCertificateCause(error)) {
            return false;
        }

        // Phase 2: check for connectivity exceptions
        Throwable current = error;
        do {
            if (current instanceof ConnectException
                    || current instanceof SocketTimeoutException
                    || current instanceof SocketException
                    || current instanceof NoRouteToHostException
                    || current instanceof UnknownHostException) {
                return true;
            }
            Throwable previous = current;
            current = current.getCause();
            // guard against self-referencing cause chains
            if (current == previous) {
                break;
            }
        } while (current != null);

        return false;
    }

    /**
     * Walks the full cause chain and returns {@code true} if any link is an
     * {@link SSLException} or {@link CertificateException} (or subclass thereof).
     *
     * <p>This correctly detects SSL errors even when they are wrapped inside
     * other exception types (e.g., an SSLException wrapped inside a ConnectException
     * by Netty).</p>
     *
     * @param error the exception to inspect; may be {@code null}
     * @return {@code true} if SSLException or CertificateException (or subclass) is
     *         anywhere in the cause chain, {@code false} otherwise or if {@code null}
     */
    public static boolean hasSslOrCertificateCause(Throwable error) {
        if (error == null) {
            return false;
        }

        Throwable current = error;
        do {
            if (current instanceof SSLException
                    || current instanceof CertificateException) {
                return true;
            }
            Throwable previous = current;
            current = current.getCause();
            if (current == previous) {
                break;
            }
        } while (current != null);

        return false;
    }
}
