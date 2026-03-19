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

import org.slf4j.Logger;

import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for logging certificate-related operations, particularly for logging
 * certificates that were ignored/filtered during client authentication.
 */
public class CertificateLoggingUtils {

    private static final String UNKNOWN = "Unknown";

    private CertificateLoggingUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Encodes the public key of an X.509 certificate to a Base64 string.
     *
     * @param cert The X.509 certificate.
     * @return The Base64 encoded string of the public key.
     */
    public static String base64EncodePublicKey(X509Certificate cert) {
        return Base64.getEncoder().encodeToString(cert.getPublicKey().getEncoded());
    }

    /**
     * Logs information about certificates that were ignored during authentication.
     * Compares the original set of certificates with the filtered set to identify ignored certificates.
     * Uses Base64-encoded public keys for reliable comparison instead of X509Certificate object equality.
     *
     * @param originalCerts The original array of certificates before filtering
     * @param filteredCerts The array of certificates after filtering for authentication
     * @param publicKeyCertificatesBase64 Set of Base64-encoded public keys of known APIML certificates
     * @param logger The logger to use for output
     */
    public static void logIgnoredCertificates(
        X509Certificate[] originalCerts,
        X509Certificate[] filteredCerts,
        Set<String> publicKeyCertificatesBase64,
        Logger logger
    ) {
        if (originalCerts == null || originalCerts.length == 0) return;

        List<X509Certificate> ignoredCerts = identifyIgnoredCertificates(
            originalCerts, filteredCerts
        );

        if (!ignoredCerts.isEmpty()) {
            logCertificateSummary(ignoredCerts, logger);
            logCertificateDetails(ignoredCerts, publicKeyCertificatesBase64, logger);
        }
    }

    /**
     * Identifies certificates that were ignored by comparing original and filtered certificate arrays.
     */
    private static List<X509Certificate> identifyIgnoredCertificates(
        X509Certificate[] originalCerts,
        X509Certificate[] filteredCerts
    ) {
        Set<String> originalKeys = Arrays.stream(originalCerts)
            .map(CertificateLoggingUtils::base64EncodePublicKey)
            .collect(Collectors.toSet());

        Set<String> filteredKeys = filteredCerts != null
            ? Arrays.stream(filteredCerts)
                .map(CertificateLoggingUtils::base64EncodePublicKey)
                .collect(Collectors.toSet())
            : new HashSet<>();

        Set<String> ignoredKeys = new HashSet<>(originalKeys);
        ignoredKeys.removeAll(filteredKeys);

        return Arrays.stream(originalCerts)
            .filter(cert -> ignoredKeys.contains(base64EncodePublicKey(cert)))
            .toList();
    }

    /**
     * Logs a summary of all ignored certificates with their key details.
     */
    private static void logCertificateSummary(
        List<X509Certificate> ignoredCerts,
        Logger logger
    ) {
        logger.debug("Certificates ignored/not used for authentication: {}",
            ignoredCerts.stream()
                .map(CertificateLoggingUtils::formatCertificateInfo)
                .collect(Collectors.joining(", ")));
    }

    /**
     * Formats certificate information for logging.
     */
    private static String formatCertificateInfo(X509Certificate cert) {
        String subjectDN = cert.getSubjectX500Principal() != null
            ? cert.getSubjectX500Principal().getName()
            : UNKNOWN;
        String issuerDN = cert.getIssuerX500Principal() != null
            ? cert.getIssuerX500Principal().getName()
            : UNKNOWN;
        String publicKeyBase64 = base64EncodePublicKey(cert);
        return String.format("[Subject: %s, Issuer: %s, Public Key (first 20 chars): %s...]",
            subjectDN, issuerDN, publicKeyBase64.substring(0, Math.min(20, publicKeyBase64.length())));
    }

    /**
     * Logs detailed information about each ignored certificate including the reason for ignoring.
     */
    private static void logCertificateDetails(
        List<X509Certificate> ignoredCerts,
        Set<String> publicKeyCertificatesBase64,
        Logger logger
    ) {
        ignoredCerts.forEach(cert -> {
            String publicKeyBase64 = base64EncodePublicKey(cert);
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

