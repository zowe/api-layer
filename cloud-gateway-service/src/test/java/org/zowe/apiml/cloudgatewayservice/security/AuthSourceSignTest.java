/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.security;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.*;

import static org.junit.jupiter.api.Assertions.*;

class AuthSourceSignTest {

    private static PublicKey publicKey;
    private static PrivateKey privateKey;
    private AuthSourceSign authSourceSign;

    @BeforeAll
    static void setup() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
    }

    @Nested
    class GivenValidKeys {

        {
            authSourceSign = new AuthSourceSign();
            ReflectionTestUtils.setField(authSourceSign, "privateKey", privateKey);
            ReflectionTestUtils.setField(authSourceSign, "publicKey", publicKey);
        }

        @Test
        void whenSigning_thenSignatureIsProduced() throws SignatureException {
            byte[] data = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes();
            byte[] signature = authSourceSign.sign(data);
            assertNotNull(signature);
            assertTrue(authSourceSign.verify(data, signature));
        }

        @Test
        void getPublicKey() {
            assertEquals(publicKey, authSourceSign.getPublicKey());
        }
    }

    @Nested
    class GivenInvalidKeys {

        {
            authSourceSign = new AuthSourceSign();
            ReflectionTestUtils.setField(authSourceSign, "privateKey", null);
            ReflectionTestUtils.setField(authSourceSign, "publicKey", null);
        }

        @Test
        void whenSigning_thenExceptionIsThrown() throws SignatureException {
            byte[] data = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes();
            SignatureException thrown = assertThrows(SignatureException.class, () -> {
                authSourceSign.sign(data);
            });
            assertEquals("java.security.InvalidKeyException: Key must not be null", thrown.getMessage());
        }

        @Test
        void whenVerifying_thenExceptionIsThrown() throws SignatureException {
            byte[] data = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes();
            byte[] signature = "12345".getBytes();
            SignatureException thrown = assertThrows(SignatureException.class, () -> {
                authSourceSign.verify(data, signature);
            });
            assertEquals("java.security.InvalidKeyException: Key must not be null", thrown.getMessage());
        }
    }
}
