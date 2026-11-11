/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zosmf.jwt.check;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.zowe.apiml.common.KeyringUtils;
import org.zowe.apiml.common.StoresNotInitializeException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ZosmfStoresTest {

    private static final String TRUSTSTORE_PATH = "../keystore/service/service.truststore.p12";
    private static final String KEYSTORE_PATH = "../keystore/service/service.keystore.p12";
    private static final String PASSWORD = "password";

    @Nested
    class GivenSafKeyring {

        @ParameterizedTest
        @CsvSource({
            "JCERACFKS, safkeyringjce",
            "JCECCARACFKS, safkeyringjcecca",
            "JCEHYBRIDRACFKS, safkeyringjcehybrid"
        })
        void whenTruststoreTypeSpecified_thenUsesMatchingProtocol(String type, String protocol) {
            ZosmfJwtCheckConfig conf = createConf(null, null, null, "safkeyring://ZWESVUSR/ZWERING", PASSWORD, type);

            // No IBM handlers on the test JVM: the failure identifies the selected protocol.
            StoresNotInitializeException e = assertThrows(StoresNotInitializeException.class, () -> new ZosmfStores(conf));
            assertEquals("unknown protocol: " + protocol, e.getMessage());
        }

        @ParameterizedTest
        @CsvSource({
            "JCERACFKS, safkeyringjce",
            "JCECCARACFKS, safkeyringjcecca",
            "JCEHYBRIDRACFKS, safkeyringjcehybrid"
        })
        void whenKeystoreTypeSpecified_thenUsesMatchingProtocol(String type, String protocol) {
            ZosmfJwtCheckConfig conf = createConf("safkeyring://ZWESVUSR/ZWERING", PASSWORD, type, null, null, null);

            StoresNotInitializeException e = assertThrows(StoresNotInitializeException.class, () -> new ZosmfStores(conf));
            assertEquals("unknown protocol: " + protocol, e.getMessage());
        }
    }

    @Nested
    class GivenDifferentKeystoreAndTruststoreTypes {

        @ParameterizedTest
        @CsvSource({
            "JCERACFKS, safkeyringjce",
            "JCECCARACFKS, safkeyringjcecca",
            "JCEHYBRIDRACFKS, safkeyringjcehybrid"
        })
        void whenKeystoreIsFileAndTruststoreIsKeyring_thenTruststoreTypeSelectsProtocol(String truststoreType, String protocol) {
            ZosmfJwtCheckConfig conf = createConf(KEYSTORE_PATH, PASSWORD, "PKCS12", "safkeyring://ZWESVUSR/ZWERING", PASSWORD, truststoreType);

            StoresNotInitializeException e = assertThrows(StoresNotInitializeException.class, () -> new ZosmfStores(conf));
            assertEquals("unknown protocol: " + protocol, e.getMessage());
        }

        @ParameterizedTest
        @CsvSource({
            "JCERACFKS, safkeyringjce",
            "JCECCARACFKS, safkeyringjcecca",
            "JCEHYBRIDRACFKS, safkeyringjcehybrid"
        })
        void whenKeystoreIsKeyringAndTruststoreIsFile_thenKeystoreTypeSelectsProtocol(String keystoreType, String protocol) {
            ZosmfJwtCheckConfig conf = createConf("safkeyring://ZWESVUSR/ZWERING", PASSWORD, keystoreType, TRUSTSTORE_PATH, PASSWORD, "PKCS12");

            StoresNotInitializeException e = assertThrows(StoresNotInitializeException.class, () -> new ZosmfStores(conf));
            assertEquals("unknown protocol: " + protocol, e.getMessage());
        }

        @Test
        void whenKeystoreAndTruststoreAreFiles_thenTruststoreIsLoadedWithItsOwnType() {
            ZosmfJwtCheckConfig conf = createConf(KEYSTORE_PATH, PASSWORD, "PKCS12", TRUSTSTORE_PATH, PASSWORD, "UNKNOWNTYPE");

            // the keystore is valid PKCS12, so the failure can only come from the truststore type
            StoresNotInitializeException e = assertThrows(StoresNotInitializeException.class, () -> new ZosmfStores(conf));
            assertEquals("UNKNOWNTYPE not found", e.getMessage());
        }
    }

    @Nested
    class GivenKeyringWithAvailableHandler {

        /** Replaces the IBM keyring access: the keyring URL points to a PKCS12 file, which is loaded as the store. */
        private MockedStatic<KeyringUtils> mockKeyring(String ringFile) throws Exception {
            MockedStatic<KeyringUtils> utils = Mockito.mockStatic(KeyringUtils.class);
            utils.when(() -> KeyringUtils.isKeyring(ArgumentMatchers.anyString()))
                .thenAnswer(i -> ((String) i.getArgument(0)).startsWith("safkeyring"));
            utils.when(() -> KeyringUtils.keyRingUrl(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(new File(ringFile).toURI().toURL());
            utils.when(() -> KeyringUtils.readKeyStore(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyString()))
                .thenAnswer(i -> {
                    KeyStore keyStore = KeyStore.getInstance("PKCS12");
                    keyStore.load(i.getArgument(0), (char[]) i.getArgument(1));
                    return keyStore;
                });
            return utils;
        }

        @Test
        void whenKeystoreIsKeyring_thenItIsAlsoUsedAsTruststore() throws Exception {
            ZosmfJwtCheckConfig conf = createConf("safkeyring://ZWESVUSR/ZWERING", PASSWORD, "JCERACFKS", null, null, null);

            try (MockedStatic<KeyringUtils> utils = mockKeyring(KEYSTORE_PATH)) {
                ZosmfStores stores = new ZosmfStores(conf);

                assertNotNull(stores.getKeyStore());
                assertSame(stores.getKeyStore(), stores.getTrustStore());
                utils.verify(() -> KeyringUtils.keyRingUrl("safkeyring://ZWESVUSR/ZWERING", "JCERACFKS"));
            }
        }

        @Test
        void whenTruststoreIsKeyring_thenItIsLoadedWithItsOwnType() throws Exception {
            ZosmfJwtCheckConfig conf = createConf(KEYSTORE_PATH, PASSWORD, "PKCS12",
                "safkeyring://ZWESVUSR/ZWERING", PASSWORD, "JCEHYBRIDRACFKS");

            try (MockedStatic<KeyringUtils> utils = mockKeyring(TRUSTSTORE_PATH)) {
                ZosmfStores stores = new ZosmfStores(conf);

                assertNotNull(stores.getKeyStore());
                assertNotNull(stores.getTrustStore());
                assertNotSame(stores.getKeyStore(), stores.getTrustStore());
                utils.verify(() -> KeyringUtils.keyRingUrl("safkeyring://ZWESVUSR/ZWERING", "JCEHYBRIDRACFKS"));
            }
        }
    }

    @Nested
    class GivenValidFileBasedTruststore {

        @Test
        void storesAreInitializedSuccessfully() {
            ZosmfJwtCheckConfig conf = createConf(null, null, TRUSTSTORE_PATH, PASSWORD, "PKCS12");
            ZosmfStores stores = new ZosmfStores(conf);

            assertNotNull(stores.getTrustStore());
        }

        @Test
        void truststoreContainsCertificates() throws Exception {
            ZosmfJwtCheckConfig conf = createConf(null, null, TRUSTSTORE_PATH, PASSWORD, "PKCS12");
            ZosmfStores stores = new ZosmfStores(conf);

            assertTrue(stores.getTrustStore().size() > 0);
        }
    }

    @Nested
    class GivenValidFileBasedKeystore {

        @Test
        void keystoreAndTruststoreAreLoaded() {
            ZosmfJwtCheckConfig conf = createConf(KEYSTORE_PATH, PASSWORD, TRUSTSTORE_PATH, PASSWORD, "PKCS12");
            ZosmfStores stores = new ZosmfStores(conf);

            assertNotNull(stores.getKeyStore());
            assertNotNull(stores.getTrustStore());
        }

        @Test
        void confIsAccessible() {
            ZosmfJwtCheckConfig conf = createConf(null, null, TRUSTSTORE_PATH, PASSWORD, "PKCS12");
            ZosmfStores stores = new ZosmfStores(conf);

            assertSame(conf, stores.getConf());
        }
    }

    @Nested
    class GivenNoTruststore {

        @Test
        void emptyTruststoreIsCreated() {
            ZosmfJwtCheckConfig conf = createConf(null, null, null, null, "PKCS12");
            ZosmfStores stores = new ZosmfStores(conf);

            assertNotNull(stores.getTrustStore());
        }
    }

    @Nested
    class GivenInvalidPaths {

        @Test
        void nonExistentTruststoreThrowsException() {
            ZosmfJwtCheckConfig conf = createConf(null, null, "/nonexistent/path.p12", PASSWORD, "PKCS12");

            StoresNotInitializeException e = assertThrows(StoresNotInitializeException.class,
                () -> new ZosmfStores(conf));
            assertNotNull(e.getMessage());
        }

        @Test
        void wrongPasswordThrowsException() {
            ZosmfJwtCheckConfig conf = createConf(null, null, TRUSTSTORE_PATH, "wrongpassword", "PKCS12");

            assertThrows(StoresNotInitializeException.class, () -> new ZosmfStores(conf));
        }

        @Test
        void nonExistentKeystoreThrowsException() {
            ZosmfJwtCheckConfig conf = createConf("/nonexistent/key.p12", PASSWORD, TRUSTSTORE_PATH, PASSWORD, "PKCS12");

            StoresNotInitializeException e = assertThrows(StoresNotInitializeException.class,
                () -> new ZosmfStores(conf));
            assertTrue(e.getMessage().contains("Error while loading keystore file"));
        }
    }

    @Nested
    class KeyringUtilsDelegation {

        @Test
        void isKeyringReturnsTrueForValidUri() {
            assertTrue(KeyringUtils.isKeyring("safkeyring://userId/keyRing"));
            assertTrue(KeyringUtils.isKeyring("safkeyring:////userId/keyRing"));
        }

        @Test
        void isKeyringReturnsFalseForNonKeyring() {
            assertFalse(KeyringUtils.isKeyring(null));
            assertFalse(KeyringUtils.isKeyring("/path/to/file.p12"));
            assertFalse(KeyringUtils.isKeyring("https://server/resource"));
        }

        @Test
        void formatKeyringUrlNormalizesSlashes() {
            assertEquals("safkeyringjce://userId/keyRing",
                KeyringUtils.formatKeyringUrl("safkeyring:////userId/keyRing", "JCERACFKS"));
            assertEquals("safkeyringjce://userId/keyRing",
                KeyringUtils.formatKeyringUrl("safkeyringjce:////userId/keyRing", "JCERACFKS"));
        }

        @Test
        void formatKeyringUrlReturnsInputForNonKeyring() {
            assertEquals("/some/path.p12", KeyringUtils.formatKeyringUrl("/some/path.p12", "PKCS12"));
            assertNull(KeyringUtils.formatKeyringUrl(null, "JCERACFKS"));
        }

        @Test
        void keyRingUrlThrowsForInvalidFormat() {
            assertThrows(StoresNotInitializeException.class,
                () -> KeyringUtils.keyRingUrl("not://a/keyring/format/extra", "JCERACFKS"));
        }

        @Test
        void keyRingUrlThrowsMalformedURLExceptionForValidButUnresolvable() {
            // Valid keyring format but no protocol handler registered
            assertThrows(MalformedURLException.class,
                () -> KeyringUtils.keyRingUrl("safkeyring://userId/keyRing", "JCERACFKS"));
        }

        @Test
        void readKeyStoreLoadsFromStream() throws Exception {
            // Create an in-memory PKCS12 keystore
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(null, "test".toCharArray());

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            ks.store(baos, "test".toCharArray());

            InputStream is = new ByteArrayInputStream(baos.toByteArray());
            KeyStore loaded = KeyringUtils.readKeyStore(is, "test".toCharArray(), "PKCS12");
            assertNotNull(loaded);
        }
    }

    private ZosmfJwtCheckConfig createConf(String keyStore, String keyStorePassword,
                                           String trustStore, String trustStorePassword,
                                           String storeType) {
        return createConf(keyStore, keyStorePassword, storeType, trustStore, trustStorePassword, storeType);
    }

    private ZosmfJwtCheckConfig createConf(String keyStore, String keyStorePassword, String keyStoreType,
                                           String trustStore, String trustStorePassword, String trustStoreType) {
        ZosmfJwtCheckConfig conf = mock(ZosmfJwtCheckConfig.class);
        when(conf.getKeyStore()).thenReturn(keyStore);
        when(conf.getKeyStorePassword()).thenReturn(keyStorePassword);
        when(conf.getKeyStoreType()).thenReturn(keyStoreType);
        when(conf.getTrustStore()).thenReturn(trustStore);
        when(conf.getTrustStorePassword()).thenReturn(trustStorePassword);
        when(conf.getTrustStoreType()).thenReturn(trustStoreType);
        return conf;
    }
}
