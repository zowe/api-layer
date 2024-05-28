/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.mapping;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.JwtAuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.X509AuthSource;
import org.zowe.apiml.zaas.utils.X509Utils;

import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.*;

class X509CommonNameUserMapperTest {

    private final X509CommonNameUserMapper x509CommonNameUserMapper = new X509CommonNameUserMapper();


    @Test
    void providedValidCertificate_returnUserId() {
        X509Certificate x509Certificate =
            X509Utils.getCertificate(X509Utils.correctBase64("zowe"), "CN=user,OU=CA CZ,O=Broadcom,L=Prague,ST=Czechia,C=CZ");
        X509AuthSource x509AuthSource = new X509AuthSource(x509Certificate);

        assertEquals("user", x509CommonNameUserMapper.mapToMainframeUserId(x509AuthSource));
    }

    @Test
    void providedInvalidCertificate_returnNull() {
        X509Certificate x509Certificate =
            X509Utils.getCertificate(X509Utils.correctBase64("zowe"), "OU=CA CZ,O=Broadcom,L=Prague,ST=Czechia,C=CZ");
        X509AuthSource x509AuthSource = new X509AuthSource(x509Certificate);

        assertNull(x509CommonNameUserMapper.mapToMainframeUserId(x509AuthSource));
    }

    @Test
    void whenWrongDN_exceptionIsThrown() {
        Exception exception = assertThrows(AuthenticationServiceException.class, () -> {
            x509CommonNameUserMapper.getLdapName("wrong DN");
        });
        assertEquals("Not able to create ldap name from certificate. Cause: Invalid name: wrong DN", exception.getMessage());
    }

    @Test
    void whenWrongAuthSource_returnNull() {
        AuthSource anotherSource = new JwtAuthSource("jwt");
        assertNull(x509CommonNameUserMapper.mapToMainframeUserId(anotherSource));
    }

}
