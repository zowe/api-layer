/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.verify;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.zowe.apiml.security.common.filter.CategorizeCertsFilter.base64EncodePublicKey;

/**
 * Service to verify if given certificate chain can be trusted.
 */
@Service
@Slf4j
public class CertificateValidator {

    final TrustedCertificatesProvider trustedCertificatesProvider;

    @Getter
    @Value("${apiml.security.x509.acceptForwardedCert:false}")
    private boolean forwardingEnabled;

    @Value("${apiml.security.x509.certificatesUrls:${apiml.security.x509.certificatesUrl:}}")
    private String[] proxyCertificatesEndpoints;
    private final Set<String> publicKeyCertificatesBase64;

    public CertificateValidator(TrustedCertificatesProvider trustedCertificatesProvider,
                                @Qualifier("publicKeyCertificatesBase64") Set<String> publicKeyCertificatesBase64) {
        this.trustedCertificatesProvider = trustedCertificatesProvider;
        this.publicKeyCertificatesBase64 = publicKeyCertificatesBase64;
    }

    /**
     * Compare given certificates with a list of trusted certs.
     *
     * @param certs Certificates to compare with known trusted ones
     * @return true if all given certificates are known false otherwise
     */
    public boolean hasGatewayChain(X509Certificate[] certs) {
        if (certs == null || certs.length == 0) return false;

        if ((proxyCertificatesEndpoints == null) || (proxyCertificatesEndpoints.length == 0)) {
            log.debug("No endpoint configured to retrieve trusted certificates. Provide URL via apiml.security.x509.certificatesUrls");
            return false;
        }
        List<Certificate> trustedCerts = Arrays.stream(proxyCertificatesEndpoints)
            .map(trustedCertificatesProvider::getTrustedCerts)
            .flatMap(List::stream)
            .toList();
        for (X509Certificate cert : certs) {

            if (isTrusted(cert, trustedCerts)) {
                continue;
            } else {
                return false;
            }
        }

        log.debug("The whole certificate chain is trusted.");
        return true;
    }

    private boolean isTrusted(X509Certificate cert, List<Certificate> trustedCerts) {
        for (var trustedCert : trustedCerts) {
            if (trustedCert instanceof X509Certificate trustedX509Cert) {
                if (cert.getSerialNumber().equals(trustedX509Cert.getSerialNumber())) {
                    log.debug("Cert is the same serial number");
                    if (!cert.equals(trustedX509Cert)) {
                        log.debug("they don't equal");
                        return false;
                    } else {
                        return true;
                    }
                }
            } else {
                log.debug("trusted cert is not an X509Certificate");
            }
        }
        return false;
    }

    /**
     * Updates the list of public keys from certificates that belong to APIML
     *
     * @param certs List of certificates coming from the central Gateway
     */
    public void updateAPIMLPublicKeyCertificates(X509Certificate[] certs) {
        for (X509Certificate cert : certs) {
            String publicKey = base64EncodePublicKey(cert);
            publicKeyCertificatesBase64.add(publicKey);
        }
    }


}
