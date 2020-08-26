/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.security.common.token;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

class X509AuthenticationTokenTest {

    private X509Certificate x509Certificate;
    private X509Certificate[] certs;
    X509AuthenticationToken token;
    X509AuthenticationToken token2;

    @BeforeEach
    void setup() {
        x509Certificate = mock(X509Certificate.class);
        certs = new X509Certificate[]{x509Certificate};
        token = new X509AuthenticationToken(certs);
        token2 = new X509AuthenticationToken(certs);
    }

    @Test
    void sameObjectsAreEquals() {
        assertEquals(token, token2);
    }

    @Test
    void objectIstNull_returnFalse() {
        assertNotEquals(token, null);
    }
}
