/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.common;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

class KeyringUtilsTest {

    @Nested
    class IsKeyring {

        @ParameterizedTest
        @ValueSource(strings = {
            "safkeyring://userId/keyRing",
            "safkeyring:////userId/keyRing",
            "safkeyringjce://userId/keyRing",
            "safkeyringjcecca://userId/keyRing",
            "safkeyringjcehybrid://user1/ring2"
        })
        void givenValidKeyringUri_thenReturnsTrue(String input) {
            assertTrue(KeyringUtils.isKeyring(input));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
            "/path/to/keystore.p12",
            "file:///some/path",
            "safkeyring:/missing-slash/ring",
            "safkeyring://",
            "safkeyring://userOnly"
        })
        void givenNonKeyringInput_thenReturnsFalse(String input) {
            assertFalse(KeyringUtils.isKeyring(input));
        }
    }

    @Nested
    class FormatKeyringUrl {

        @ParameterizedTest
        @CsvSource({
            "safkeyring://userId/keyRing, safkeyring://userId/keyRing",
            "safkeyring:////userId/keyRing, safkeyring://userId/keyRing",
            "safkeyringjce://user/ring, safkeyringjce://user/ring",
            "safkeyringjcecca:///user1/ring2, safkeyringjcecca://user1/ring2"
        })
        void givenKeyringUri_thenNormalizesToTwoSlashes(String input, String expected) {
            assertEquals(expected, KeyringUtils.formatKeyringUrl(input));
        }

        @Test
        void givenNonKeyringPath_thenReturnsUnchanged() {
            String path = "/path/to/keystore.p12";
            assertEquals(path, KeyringUtils.formatKeyringUrl(path));
        }

        @Test
        void givenNull_thenReturnsNull() {
            assertNull(KeyringUtils.formatKeyringUrl(null));
        }
    }

    @Nested
    class KeyRingUrl {

        @Test
        void givenInvalidKeyringFormat_thenThrowsStoresNotInitializeException() {
            assertThrows(StoresNotInitializeException.class,
                () -> KeyringUtils.keyRingUrl("/some/file/path"));
        }

        @Test
        void givenInvalidFormat_thenExceptionContainsHelpfulMessage() {
            StoresNotInitializeException ex = assertThrows(StoresNotInitializeException.class,
                () -> KeyringUtils.keyRingUrl("notakeyring"));
            assertThat(ex.getMessage(), containsString("Incorrect key ring format"));
            assertThat(ex.getMessage(), containsString("safkeyring://userId/keyRing"));
        }

        @Test
        void givenNullInput_thenThrowsStoresNotInitializeException() {
            assertThrows(StoresNotInitializeException.class,
                () -> KeyringUtils.keyRingUrl(null));
        }

        @Test
        void givenValidKeyringFormat_thenThrowsMalformedURLExceptionWithoutHandler() {
            // Valid keyring format but no SAF protocol handler registered on this JVM
            java.net.MalformedURLException ex = assertThrows(java.net.MalformedURLException.class,
                () -> KeyringUtils.keyRingUrl("safkeyring://userId/keyRing"));
            assertThat(ex.getMessage(), containsString("unknown protocol"));
        }
    }

    @Nested
    class ReadKeyStore {

        @Test
        void givenValidPKCS12Stream_thenLoadsKeyStore() throws Exception {
            // Use the test keystore from the project
            String truststorePath = "../keystore/service/service.truststore.p12";
            java.io.File file = new java.io.File(truststorePath);
            if (!file.exists()) {
                // Skip if keystore not available in this environment
                return;
            }

            try (InputStream is = new java.io.FileInputStream(file)) {
                KeyStore ks = KeyringUtils.readKeyStore(is, "password".toCharArray(), "PKCS12");
                assertNotNull(ks);
                assertTrue(ks.size() > 0);
            }
        }

        @Test
        void givenInvalidPassword_thenThrowsIOException() throws Exception {
            String truststorePath = "../keystore/service/service.truststore.p12";
            java.io.File file = new java.io.File(truststorePath);
            if (!file.exists()) {
                return;
            }

            try (InputStream is = new java.io.FileInputStream(file)) {
                assertThrows(IOException.class,
                    () -> KeyringUtils.readKeyStore(is, "wrongpassword".toCharArray(), "PKCS12"));
            }
        }

        @Test
        void givenInvalidType_thenThrowsKeyStoreException() {
            InputStream is = new ByteArrayInputStream(new byte[0]);
            assertThrows(KeyStoreException.class,
                () -> KeyringUtils.readKeyStore(is, "pass".toCharArray(), "INVALID_TYPE"));
        }
    }

    @Nested
    class EnsureSafkeyringHandler {

        @Test
        void givenEmptyProperty_thenSetsHandlerPackages() {
            String original = System.getProperty("java.protocol.handler.pkgs", "");
            try {
                System.setProperty("java.protocol.handler.pkgs", "");

                KeyringUtils.ensureSafkeyringHandler();

                String result = System.getProperty("java.protocol.handler.pkgs");
                assertThat(result, containsString("com.ibm.crypto.zsecurity.provider"));
                assertThat(result, containsString("com.ibm.crypto.hdwrCCA.provider"));
            } finally {
                System.setProperty("java.protocol.handler.pkgs", original);
            }
        }

        @Test
        void givenExistingPackages_thenAppendsWithPipeSeparator() {
            String original = System.getProperty("java.protocol.handler.pkgs", "");
            try {
                System.setProperty("java.protocol.handler.pkgs", "com.example.handler");

                KeyringUtils.ensureSafkeyringHandler();

                String result = System.getProperty("java.protocol.handler.pkgs");
                assertThat(result, startsWith("com.example.handler|"));
                assertThat(result, containsString("com.ibm.crypto.zsecurity.provider"));
                assertThat(result, containsString("com.ibm.crypto.hdwrCCA.provider"));
            } finally {
                System.setProperty("java.protocol.handler.pkgs", original);
            }
        }

        @Test
        void givenAlreadyRegistered_thenDoesNotDuplicate() {
            String original = System.getProperty("java.protocol.handler.pkgs", "");
            try {
                System.setProperty("java.protocol.handler.pkgs", "");

                KeyringUtils.ensureSafkeyringHandler();
                String firstRun = System.getProperty("java.protocol.handler.pkgs");

                KeyringUtils.ensureSafkeyringHandler();
                String secondRun = System.getProperty("java.protocol.handler.pkgs");

                assertEquals(firstRun, secondRun);
            } finally {
                System.setProperty("java.protocol.handler.pkgs", original);
            }
        }
    }

    @Nested
    class StoresNotInitializeExceptionTest {

        @Test
        void givenMessage_thenExceptionContainsIt() {
            StoresNotInitializeException ex = new StoresNotInitializeException("test error");
            assertEquals("test error", ex.getMessage());
        }

        @Test
        void givenException_thenIsRuntimeException() {
            StoresNotInitializeException ex = new StoresNotInitializeException("msg");
            assertThat(ex, instanceOf(RuntimeException.class));
        }
    }
}
