/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.util;

import lombok.experimental.UtilityClass;
import org.slf4j.Logger;

import java.security.cert.X509Certificate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class for logging certificate-related operations, particularly for logging
 * certificates that were ignored/filtered during client authentication.
 */
@UtilityClass
public class CertificateLoggingUtils {

    private static final String UNKNOWN = "Unknown";

    /**
     * Logs information about certificates that were ignored during authentication.
     * Compares the original set of certificates with the filtered set to identify ignored certificates.
     * Uses Base64-encoded public keys for reliable comparison instead of X509Certificate object equality.
     *
     * @param originalCerts The original array of certificates before filtering
     * @param filteredCerts The array of certificates after filtering for authentication
     * @param publicKeyCertificatesBase64 Set of Base64-encoded public keys of known APIML certificates
     * @param logger The logger to use for output
     * @param base64Encoder Function to encode certificate public key to Base64
     */
    public static void logIgnoredCertificates(
        X509Certificate[] originalCerts,
        X509Certificate[] filteredCerts,
        Set<String> publicKeyCertificatesBase64,
        Logger logger,
        Function<X509Certificate, String> base64Encoder
    ) {
        if (originalCerts == null || originalCerts.length == 0) return;

        List<X509Certificate> ignoredCerts = identifyIgnoredCertificates(
            originalCerts, filteredCerts, base64Encoder
        );

        if (!ignoredCerts.isEmpty()) {
            logCertificateSummary(ignoredCerts, base64Encoder, logger);
            logCertificateDetails(ignoredCerts, publicKeyCertificatesBase64, base64Encoder, logger);
        }
    }

    /**
     * Identifies certificates that were ignored by comparing original and filtered certificate arrays.
     */
    private static List<X509Certificate> identifyIgnoredCertificates(
        X509Certificate[] originalCerts,
        X509Certificate[] filteredCerts,
        Function<X509Certificate, String> base64Encoder
    ) {
        Set<String> originalKeys = Arrays.stream(originalCerts)
            .map(base64Encoder)
            .collect(Collectors.toSet());

        Set<String> filteredKeys = filteredCerts != null
            ? Arrays.stream(filteredCerts)
                .map(base64Encoder)
                .collect(Collectors.toSet())
            : new HashSet<>();

        Set<String> ignoredKeys = new HashSet<>(originalKeys);
        ignoredKeys.removeAll(filteredKeys);

        return Arrays.stream(originalCerts)
            .filter(cert -> ignoredKeys.contains(base64Encoder.apply(cert)))
            .toList();
    }

    /**
     * Logs a summary of all ignored certificates with their key details.
     */
    private static void logCertificateSummary(
        List<X509Certificate> ignoredCerts,
        Function<X509Certificate, String> base64Encoder,
        Logger logger
    ) {
        logger.debug("Certificates ignored/not used for authentication: {}",
            ignoredCerts.stream()
                .map(cert -> formatCertificateInfo(cert, base64Encoder))
                .collect(Collectors.joining(", ")));
    }

    /**
     * Formats certificate information for logging.
     */
    private static String formatCertificateInfo(
        X509Certificate cert,
        Function<X509Certificate, String> base64Encoder
    ) {
        String subjectDN = cert.getSubjectX500Principal() != null
            ? cert.getSubjectX500Principal().getName()
            : UNKNOWN;
        String issuerDN = cert.getIssuerX500Principal() != null
            ? cert.getIssuerX500Principal().getName()
            : UNKNOWN;
        String publicKeyBase64 = base64Encoder.apply(cert);
        return String.format("[Subject: %s, Issuer: %s, Public Key (first 20 chars): %s...]",
            subjectDN, issuerDN, publicKeyBase64.substring(0, Math.min(20, publicKeyBase64.length())));
    }

    /**
     * Logs detailed information about each ignored certificate including the reason for ignoring.
     */
    private static void logCertificateDetails(
        List<X509Certificate> ignoredCerts,
        Set<String> publicKeyCertificatesBase64,
        Function<X509Certificate, String> base64Encoder,
        Logger logger
    ) {
        ignoredCerts.forEach(cert -> {
            String publicKeyBase64 = base64Encoder.apply(cert);
            boolean isApimlCert = publicKeyCertificatesBase64.contains(publicKeyBase64);
            String subjectDN = cert.getSubjectX500Principal() != null
                ? cert.getSubjectX500Principal().getName()
                : UNKNOWN;
            if (isApimlCert) {
                logger.debug("Certificate with subject '{}' was ignored because it is an APIML Gateway certificate (not used for client authentication)",
                    subjectDN);
            } else {
                logger.debug("Certificate with subject '{}' was ignored for unknown reason (not in APIML cert set, but filtered by predicate)",
                    subjectDN);
            }
        });
    }
}

