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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Service to retrieve the public key during initialization of CertificateValidator bean. The public key is then used to verify the
 * certificate's signature provided on the request header.
 */
@Service
@Slf4j
public class CertificateValidator {

    final TrustedCertificatesProvider trustedCertificatesProvider;

    @Getter
    @Value("${apiml.security.x509.authViaHeader:false}")
    private boolean certInHeader;

    @Value("${apiml.security.x509.certificatesUrl:}")
    private String proxyCertificatesEndpoint;

    @Autowired
    public CertificateValidator(TrustedCertificatesProvider trustedCertificatesProvider) {
        this.trustedCertificatesProvider = trustedCertificatesProvider;
    }

    public boolean compareWithTrustedCerts(X509Certificate[] certs) {
        List<Certificate> trustedCerts = trustedCertificatesProvider.getTrustedCerts(proxyCertificatesEndpoint);
        for (X509Certificate cert : certs) {
            if (!trustedCerts.contains(cert)) {
                return false;
            }
        }
        return true;
    }
}
