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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;

/**
 * Service to verify if given certificate chain can be trusted.
 */
@Service
@Slf4j
public class CertificateValidator {

    final TrustedCertificatesProvider trustedCertificatesProvider;

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    @Getter
    @Value("${apiml.security.x509.acceptForwardedCert:false}")
    private boolean forwardingEnabled;

    @Value("${apiml.security.x509.certificatesUrl:}")
    private String proxyCertificatesEndpoint;
    private final Set<String> publicKeyCertificatesBase64;


    @Autowired
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
    public boolean isTrusted(X509Certificate[] certs) {
        List<Certificate> trustedCerts = StringUtils.isBlank(proxyCertificatesEndpoint) ? emptyList() : trustedCertificatesProvider.getTrustedCerts(proxyCertificatesEndpoint);
        for (X509Certificate cert : certs) {
            if (!trustedCerts.contains(cert)) {
                apimlLog.log("org.zowe.apiml.security.common.verify.untrustedCert");
                log.debug("Untrusted certificate is {}", cert);
                return false;
            }
        }
        log.debug("All certificates are trusted.");
        return true;
    }

    /**
     * Updates the list of public keys from certificates that belong to APIML
     *
     * @param certs List of certificates coming from the central Gateway
     */
    public void updateAPIMLPublicKeyCertificates(X509Certificate[] certs) {
        for (X509Certificate cert : certs) {
            String publicKey = Base64.getEncoder().encodeToString(cert.getPublicKey().getEncoded());
            publicKeyCertificatesBase64.add(publicKey);
        }
    }
}
