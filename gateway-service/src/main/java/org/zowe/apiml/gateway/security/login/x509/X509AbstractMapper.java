/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.login.x509;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationServiceException;

import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.List;

@Slf4j
public abstract class X509AbstractMapper implements X509AuthenticationMapper {

    private static final String CLIENT_AUTH_OID = "1.3.6.1.5.5.7.3.2";

    /**
     * Verify that the certificate is valid and that contains the OID for client authentication
     *
     * @param certificate
     * @return
     */
    public boolean isClientAuthCertificate(X509Certificate certificate) {
        List<String> extendedKeyUsage;
        try {
            extendedKeyUsage = certificate.getExtendedKeyUsage();
        } catch (CertificateParsingException e) {
            throw new AuthenticationServiceException("Can't get extensions from certificate");
        }
        if (extendedKeyUsage == null) {
            log.debug("X509 certificate is missing the client certificate extended usage definition");
            return false;
        }
        final boolean containsOID = extendedKeyUsage.contains(CLIENT_AUTH_OID);

        if (containsOID) {
            log.debug("X509 certificate does contain the client certificate extended usage definition.");
        } else {
            log.debug("X509 certificate is missing the client certificate extended usage definition.");
        }

        return containsOID;
    }
}
