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
import java.security.cert.X509Certificate;
import java.util.Base64;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@UtilityClass
public class X509Utils {
    public X509Certificate getCertificate(String base64) {
        X509Certificate out = mock(X509Certificate.class);
        PublicKey publicKey = mock(PublicKey.class);
        doReturn(publicKey).when(out).getPublicKey();
        doReturn(Base64.getDecoder().decode(base64)).when(publicKey).getEncoded();
        doReturn(new X500Principal("CN=zowe user")).when(out).getSubjectDN();
        return out;
    }
    public String correctBase64(String i) {
        return new String(
            Base64.getEncoder().encode(i.getBytes())
        );
    }
}
