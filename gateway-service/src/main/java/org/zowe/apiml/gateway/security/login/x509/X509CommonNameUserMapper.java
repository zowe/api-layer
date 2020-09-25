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
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Component;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.security.cert.X509Certificate;

/**
 * Certificate mapper that allows to return user id of the provided x509 certificate
 * This mapper will be executed when ZSS is not used
 */
@Slf4j
@Component
@ConditionalOnExpression("T(org.springframework.util.StringUtils).isEmpty('${apiml.security.x509.externalMapperUrl}')"
)
public class X509CommonNameUserMapper extends X509AbstractMapper {


    /**
     * Maps certificate to user id
     *
     * @param certificate
     * @return the user
     */
    public String mapCertificateToMainframeUserId(X509Certificate certificate) {
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


    /**
     * Return the LDAP name from the given distinguished name
     *
     * @param dn distinguished name
     * @return LDAP name
     */
    public LdapName getLdapName(String dn) {
        try {
            return new LdapName(dn);
        } catch (InvalidNameException e) {
            throw new AuthenticationServiceException("Not able to create ldap name from certificate. Cause: " + e.getMessage(), e);
        }
    }
}
