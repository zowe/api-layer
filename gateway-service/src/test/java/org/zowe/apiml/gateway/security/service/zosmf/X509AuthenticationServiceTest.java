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

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.zowe.apiml.gateway.utils.X509Utils;

import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.*;


class X509AuthenticationServiceTest {

    private final X509AuthenticationService x509AuthenticationService = new X509AuthenticationService();


    @Test
    void providedValidCertificate_returnUserId() {
        X509Certificate x509Certificate =
            X509Utils.getCertificate(X509Utils.correctBase64("zowe"), "CN=user,OU=CA CZ,O=Broadcom,L=Prague,ST=Czechia,C=CZ");

        assertEquals("user", x509AuthenticationService.mapUserToCertificate(x509Certificate));
    }

    @Test
    void providedInvalidCertificate_returnNull() {
        X509Certificate x509Certificate =
            X509Utils.getCertificate(X509Utils.correctBase64("zowe"), "OU=CA CZ,O=Broadcom,L=Prague,ST=Czechia,C=CZ");

        assertNull(x509AuthenticationService.mapUserToCertificate(x509Certificate));
    }

    @Test
    void whenWrongDN_exceptionIsThrown() {
        Exception exception = assertThrows(AuthenticationServiceException.class, () -> {
            x509AuthenticationService.getLdapName("wrong DN");
        });
        assertEquals("Not able to create ldap name from certificate. Cause: Invalid name: wrong DN", exception.getMessage());
    }

    @Test
    void rejectZoweServiceCertificate() {
        X509Certificate x509Certificate =
            X509Utils.getCertificate(X509Utils.correctBase64("zowe"), "CN=Zowe Service,OU=CA CZ,O=Broadcom,L=Prague,ST=Czechia,C=CZ");

        assertNull(x509AuthenticationService.mapUserToCertificate(x509Certificate));
    }
}
