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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.ServiceConfigurationError;

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
            "safkeyring://userId/keyRing, JCERACFKS, safkeyringjce://userId/keyRing",
            "safkeyring:///userId/keyRing, JCECCARACFKS, safkeyringjcecca://userId/keyRing",
            "safkeyring:////ZWESVUSR/ZWEHRING, JCEHYBRIDRACFKS, safkeyringjcehybrid://ZWESVUSR/ZWEHRING",
            "safkeyring://userId/keyRing, jcehybridracfks, safkeyringjcehybrid://userId/keyRing",
            "safkeyringjcehybrid://userId/keyRing, JCEHYBRIDRACFKS, safkeyringjcehybrid://userId/keyRing",
            "safkeyringjcecca:////userId/keyRing, JCEHYBRIDRACFKS, safkeyringjcecca://userId/keyRing",
            "safkeyring://userId/keyRing, PKCS12, safkeyring://userId/keyRing",
            "safkeyring://userId/keyRing, , safkeyring://userId/keyRing",
            "/path/to/truststore.p12, PKCS12, /path/to/truststore.p12",
            "safkeyring:/invalid/ring, JCEHYBRIDRACFKS, safkeyring:/invalid/ring"
        })
        void givenStoreType_thenSelectsProtocolOnlyForGenericKeyringUri(String input, String type, String expected) {
            assertEquals(expected, KeyringUtils.formatKeyringUrl(input, type));
        }

        @Test
        void givenNullWithStoreType_thenReturnsNull() {
            assertNull(KeyringUtils.formatKeyringUrl(null, "JCEHYBRIDRACFKS"));
        }

        @ParameterizedTest
        @CsvSource({
            "safkeyring://userId/keyRing, safkeyring://userId/keyRing",
            "safkeyring:////userId/keyRing, safkeyring://userId/keyRing",
            "safkeyringjce://user/ring, safkeyringjce://user/ring",
            "safkeyringjcecca:///user1/ring2, safkeyringjcecca://user1/ring2"
        })
        void givenKeyringUri_thenNormalizesToTwoSlashes(String input, String expected) {
            assertEquals(expected, KeyringUtils.formatKeyringUrl(input, "PKCS12"));
        }

        @Test
        void givenNonKeyringPath_thenReturnsUnchanged() {
            String path = "/path/to/keystore.p12";
            assertEquals(path, KeyringUtils.formatKeyringUrl(path, "PKCS12"));
        }

        @Test
        void givenNull_thenReturnsNull() {
            assertNull(KeyringUtils.formatKeyringUrl(null, "PKCS12"));
        }
    }

    @Nested
    class KeyRingUrl {

        @Test
        void givenInvalidKeyringFormat_thenThrowsStoresNotInitializeException() {
            assertThrows(StoresNotInitializeException.class,
                () -> KeyringUtils.keyRingUrl("/some/file/path", "JCERACFKS"));
        }

        @Test
        void givenInvalidFormat_thenExceptionContainsHelpfulMessage() {
            StoresNotInitializeException ex = assertThrows(StoresNotInitializeException.class,
                () -> KeyringUtils.keyRingUrl("notakeyring", "JCERACFKS"));
            assertThat(ex.getMessage(), containsString("Incorrect key ring format"));
            assertThat(ex.getMessage(), containsString("safkeyring://userId/keyRing"));
        }

        @Test
        void givenNullInput_thenThrowsStoresNotInitializeException() {
            assertThrows(StoresNotInitializeException.class,
                () -> KeyringUtils.keyRingUrl(null, "JCERACFKS"));
        }

        @Test
        void givenValidKeyringFormat_thenThrowsMalformedURLExceptionWithoutHandler() {
            // Valid keyring format but no SAF protocol handler registered on this JVM
            java.net.MalformedURLException ex = assertThrows(java.net.MalformedURLException.class,
                () -> KeyringUtils.keyRingUrl("safkeyring://userId/keyRing", "JCERACFKS"));
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

    @Nested
    class SecurityProviders {

        private static final String A = "ZoweTestProviderA";
        private static final String B = "ZoweTestProviderB";

        private PrintStream originalErr;
        private ByteArrayOutputStream err;

        @BeforeEach
        void captureStdErr() {
            originalErr = System.err;
            err = new ByteArrayOutputStream();
            System.setErr(new PrintStream(err, true));
        }

        @AfterEach
        void cleanUp() {
            System.setErr(originalErr);
            Security.removeProvider(A);
            Security.removeProvider(B);
        }

        private Iterator<Provider> candidates(Object... items) {
            Deque<Object> queue = new ArrayDeque<>(List.of(items));
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return !queue.isEmpty();
                }

                @Override
                public Provider next() {
                    Object item = queue.poll();
                    if (item instanceof ServiceConfigurationError e) throw e;
                    if (item instanceof RuntimeException e) throw e;
                    return (Provider) item;
                }
            };
        }

        private Provider provider(String name) {
            return new Provider(name, "1.0", "test provider") {
            };
        }

        private int indexOf(String name) {
            Provider[] providers = Security.getProviders();
            for (int i = 0; i < providers.length; i++) {
                if (name.equals(providers[i].getName())) return i;
            }
            return -1;
        }

        @Test
        void configuredProviderNamesAreReadInOrderFromSharedFile() {
            List<String> names = KeyringUtils.configuredProviderNames();
            assertEquals("IBMJCEHYBRID", names.get(0));
            assertEquals("IBMJCECCA", names.get(1));
            assertTrue(names.contains("SUN"));
        }

        @Test
        void givenMissingProviders_thenRegisteredInConfiguredOrder() {
            KeyringUtils.registerProviders(List.of(A, B), candidates(provider(B), provider(A)));

            assertEquals(0, indexOf(A));
            assertEquals(1, indexOf(B));
        }

        @Test
        void givenAlreadyRegistered_thenNothingChangesWhenCalledAgain() {
            Provider first = provider(A);
            KeyringUtils.registerProviders(List.of(A), candidates(first));
            int count = Security.getProviders().length;

            KeyringUtils.registerProviders(List.of(A), candidates(provider(A)));

            assertEquals(count, Security.getProviders().length);
            assertSame(first, Security.getProvider(A));
        }

        @Test
        void givenFailingProvider_thenFollowingProvidersAreStillRegistered() {
            KeyringUtils.registerProviders(List.of(A), candidates(
                new ServiceConfigurationError("cannot be instantiated"),
                new IllegalStateException("broken"),
                provider(A)));

            assertNotNull(Security.getProvider(A));
            assertThat(err.toString(), containsString("cannot be instantiated"));
            assertThat(err.toString(), containsString("broken"));
        }

        @Test
        void givenIteratorFailingForever_thenGivesUp() {
            Iterator<Provider> broken = new Iterator<>() {
                @Override
                public boolean hasNext() {
                    throw new ServiceConfigurationError("always");
                }

                @Override
                public Provider next() {
                    throw new NoSuchElementException();
                }
            };

            assertDoesNotThrow(() -> KeyringUtils.registerProviders(List.of(A), broken));
            assertNull(Security.getProvider(A));
        }

        @Test
        void givenProviderNotAvailable_thenWarnsAndRegistersTheOthers() {
            KeyringUtils.registerProviders(List.of(A, B), candidates(provider(B)));

            assertNotNull(Security.getProvider(B));
            assertNull(Security.getProvider(A));
            assertThat(err.toString(), containsString("WARN"));
            assertThat(err.toString(), containsString(A));
            assertThat(err.toString(), not(containsString("[" + A + ", " + B + "]")));
        }

        @Test
        void givenAllProvidersRegistered_thenDoesNotLoadCandidates() {
            KeyringUtils.registerProviders(List.of("SUN"), candidates(new IllegalStateException("must not be touched")));

            assertEquals("", err.toString());
        }

        @ParameterizedTest
        @CsvSource({
            "JCERACFKS, true",
            "JCECCARACFKS, true",
            "JCEHYBRIDRACFKS, true",
            "jcehybridracfks, true",
            "JCECCAKS, true",
            "PKCS12, false",
            "JKS, false",
            "JCEKS, false"
        })
        void givenStoreType_thenDecidesWhetherIbmProvidersAreNeeded(String type, boolean expected) {
            assertEquals(expected, KeyringUtils.isIbmProviderStoreType(type));
        }

        @Test
        void givenNullStoreType_thenIbmProvidersAreNotNeeded() {
            assertFalse(KeyringUtils.isIbmProviderStoreType(null));
        }
    }
}
