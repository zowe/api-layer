/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.utils;

import lombok.experimental.UtilityClass;

import javax.security.auth.x500.X500Principal;
import java.security.PublicKey;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collections;

import static org.mockito.Mockito.*;

@UtilityClass
public class X509Utils {
    public X509Certificate getCertificate(String base64) {
      return getCertificate(base64, "CN=zowe user");
    }
    public String correctBase64(String i) {
        return new String(
            Base64.getEncoder().encode(i.getBytes())
        );
    }

    public X509Certificate getCertificate(String base64, String CN) {
        X509Certificate out = mock(X509Certificate.class);
        PublicKey publicKey = mock(PublicKey.class);
        doReturn(publicKey).when(out).getPublicKey();
        doReturn(new X500Principal(CN))
            .when(out).getSubjectX500Principal();
        doReturn(Base64.getDecoder().decode(base64)).when(publicKey).getEncoded();
        try {
            doReturn(Collections.singletonList("1.3.6.1.5.5.7.3.2")).when(out).getExtendedKeyUsage();
        } catch (CertificateParsingException e) {
           throw new RuntimeException("Problems mocking key extensions");
        }
        doReturn(new X500Principal(CN)).when(out).getSubjectDN();
        return out;
    }
}
