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

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.zowe.apiml.gateway.utils.X509Utils;

import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

class X509CommonNameUserMapperTest {

    private final X509CommonNameUserMapper x509CommonNameUserMapper = new X509CommonNameUserMapper();


    @Test
    void providedValidCertificate_returnUserId() {
        X509Certificate x509Certificate =
            X509Utils.getCertificate(X509Utils.correctBase64("zowe"), "CN=user,OU=CA CZ,O=Broadcom,L=Prague,ST=Czechia,C=CZ");

        assertEquals("user", x509CommonNameUserMapper.mapCertificateToMainframeUserId(x509Certificate));
    }

    @Test
    void providedValidCertificateWithUpperOrMixedCase_returnUserIdInLowercase() {
        X509Certificate x509Certificate =
            X509Utils.getCertificate(X509Utils.correctBase64("zowe"), "CN=UsER,OU=CA CZ,O=Broadcom,L=Prague,ST=Czechia,C=CZ");

        assertEquals("user", x509CommonNameUserMapper.mapCertificateToMainframeUserId(x509Certificate));
    }

    @Test
    void providedInvalidCertificate_returnNull() {
        X509Certificate x509Certificate =
            X509Utils.getCertificate(X509Utils.correctBase64("zowe"), "OU=CA CZ,O=Broadcom,L=Prague,ST=Czechia,C=CZ");

        assertNull(x509CommonNameUserMapper.mapCertificateToMainframeUserId(x509Certificate));
    }

    @Test
    void whenWrongDN_exceptionIsThrown() {
        Exception exception = assertThrows(AuthenticationServiceException.class, () -> {
            x509CommonNameUserMapper.getLdapName("wrong DN");
        });
        assertEquals("Not able to create ldap name from certificate. Cause: Invalid name: wrong DN", exception.getMessage());
    }

    @Test
    void whenWrongExtension_throwException() {
        X509Certificate x509Certificate =
            X509Utils.getCertificate(X509Utils.correctBase64("zowe"), "CN=user,OU=CA CZ,O=Broadcom,L=Prague,ST=Czechia,C=CZ");
        try {
            doThrow(new CertificateParsingException()).when(x509Certificate).getExtendedKeyUsage();
        } catch (CertificateParsingException e) {
            throw new RuntimeException("Error mocking exception");
        }
        Exception exception = assertThrows(AuthenticationServiceException.class, () -> x509CommonNameUserMapper.isClientAuthCertificate(x509Certificate));
        assertEquals("Can't get extensions from certificate", exception.getMessage());
    }

    @Test
    void whenNullExtension_thenReturnFalse() {
        X509Certificate x509Certificate =
            X509Utils.getCertificate(X509Utils.correctBase64("zowe"), "CN=user,OU=CA CZ,O=Broadcom,L=Prague,ST=Czechia,C=CZ");
        try {
            doReturn(null).when(x509Certificate).getExtendedKeyUsage();
        } catch (CertificateParsingException e) {
            throw new RuntimeException("Error mocking exception");
        }
        assertFalse(x509CommonNameUserMapper.isClientAuthCertificate(x509Certificate));
    }

}
