/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

class TomcatKeyringFixTest {

    private static final String PASSWORD = "psswd";
    private static final char[] PASSWORD_CHAR_ARRAY = PASSWORD.toCharArray();
    private static final String DEFAULT_PASSWORD = "password";

    private final Ssl ssl = mock(Ssl.class);
    private final TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();

    @BeforeEach
    void setUp() {
        factory.setSsl(ssl);
    }

    private void customize(
        String keyStore, char[] keyStorePassword, char[] keyPassword, String keyAlias,
        String trustStore, char[] trustStorePassword
    ) {
        TomcatKeyringFix customizer = new TomcatKeyringFix();
        ReflectionTestUtils.setField(customizer, "keyAlias", keyAlias);
        ReflectionTestUtils.setField(customizer, "keyStore", keyStore);
        ReflectionTestUtils.setField(customizer, "keyStorePassword", keyStorePassword);
        ReflectionTestUtils.setField(customizer, "keyPassword", keyPassword);
        ReflectionTestUtils.setField(customizer, "trustStore", trustStore);
        ReflectionTestUtils.setField(customizer, "trustStorePassword", trustStorePassword);

        customizer.customize(factory);
    }

    @Nested
    class UsingKeyring {

        @Test
        void whenInvalidFormatAndMissingPassword_thenFixIt() {
            customize("safkeyring:///userId/ringIdKs", null, null, "alias", "safkeyringpce:////userId/ringIdTs", null);

            verify(ssl).setKeyAlias("alias");
            verify(ssl).setKeyStore("safkeyring://userId/ringIdKs");
            verify(ssl).setKeyStorePassword(DEFAULT_PASSWORD);
            verify(ssl).setKeyPassword(DEFAULT_PASSWORD);

            verify(ssl).setTrustStore("safkeyringpce://userId/ringIdTs");
            verify(ssl).setTrustStorePassword(DEFAULT_PASSWORD);
        }

        @Test
        void whenPasswordSet_thenDontUpdate() {
            customize("safkeyring:///userId/ringIdKs", PASSWORD_CHAR_ARRAY, PASSWORD_CHAR_ARRAY, "alias2", "safkeyringpce:////userId/ringIdTs", PASSWORD_CHAR_ARRAY);

            verify(ssl).setKeyStorePassword(PASSWORD);
            verify(ssl).setKeyPassword(PASSWORD);
            verify(ssl).setTrustStorePassword(PASSWORD);
        }

    }

    @Nested
    class UsingKeystore {

        @Test
        void dontUpdate() {
            customize("/somePath", PASSWORD_CHAR_ARRAY, PASSWORD_CHAR_ARRAY, "alias", "/anotherPath", PASSWORD_CHAR_ARRAY);

            verify(ssl, never()).setKeyAlias(any());
            verify(ssl, never()).setKeyStore(any());
            verify(ssl, never()).setKeyStorePassword(any());
            verify(ssl, never()).setKeyPassword(any());

            verify(ssl, never()).setTrustStore(any());
            verify(ssl, never()).setTrustStorePassword(any());
        }
    }

}
