/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service.zosmf;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.zowe.apiml.gateway.security.login.x509.X509Authentication;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.List;

@Slf4j
public class X509AuthenticationService implements X509Authentication {

    private static final String CLIENT_AUTH_OID = "1.3.6.1.5.5.7.3.2";

    public String mapUserToCertificate(X509Certificate certificate) {
        if (isClientAuthCertificate(certificate)) {
            String dn = certificate.getSubjectX500Principal().getName();
            LdapName ldapDN = getLdapName(dn);
            for (Rdn rdn : ldapDN.getRdns()) {
                if ("cn".equalsIgnoreCase(rdn.getType())) {
                    return String.valueOf(rdn.getValue());
                }
            }
        }
        return null;
    }

    public boolean isClientAuthCertificate(X509Certificate certificate) {
        List<String> extendedKeyUsage;
        try {
            extendedKeyUsage = certificate.getExtendedKeyUsage();
        } catch (CertificateParsingException e) {
            throw new AuthenticationServiceException("Can't get extensions from certificate");
        }
        if (extendedKeyUsage == null) {
            return false;
        }
        return extendedKeyUsage.contains(CLIENT_AUTH_OID);
    }


    public LdapName getLdapName(String dn) {
        try {
            return new LdapName(dn);
        } catch (InvalidNameException e) {
            throw new AuthenticationServiceException("Not able to create ldap name from certificate. Cause: " + e.getMessage(), e);
        }
    }
}
