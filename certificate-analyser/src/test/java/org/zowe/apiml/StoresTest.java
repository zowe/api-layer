/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.common.KeyringUtils;
import org.zowe.apiml.common.StoresNotInitializeException;
import picocli.CommandLine;

import java.net.MalformedURLException;

import static org.junit.jupiter.api.Assertions.*;

class StoresTest {

    @Nested
    class GivenWrongPassword {
        @Test
        void whenExecuteCommand_thenStoresNotInitializeExceptionIsThrown() {
            String[] args = {"--keystore", "../keystore/localhost/localhost.keystore.p12",
                "--truststore", "../keystore/localhost/localhost.truststore.p12",
                "--keypasswd", "wrongPass",
                "--keyalias", "localhost"};
            ApimlConf conf = new ApimlConf();
            new CommandLine(conf).parseArgs(args);
            StoresNotInitializeException e = assertThrows(StoresNotInitializeException.class, () -> new Stores(conf));
            assertEquals("keystore password was incorrect",e.getMessage());
        }
    }

    @Nested
    class GivenWrongTrustStorePath {
        @Test
        void whenExecuteCommand_thenStoresNotInitializeExceptionIsThrown() {
            String[] args = {"--keystore", "../keystore/localhost/localhost.keystore.p12",
                "--truststore", "../wrongPath/localhost.truststore.p12",
                "--keypasswd", "password",
                "--keyalias", "localhost"};
            ApimlConf conf = new ApimlConf();
            new CommandLine(conf).parseArgs(args);
            StoresNotInitializeException e = assertThrows(StoresNotInitializeException.class, () -> new Stores(conf));
            String message = e.getMessage().replace("\\wrongPath\\", "/wrongPath/"); // replace to fix issue on windows
            assertTrue(message.contains("Error while loading keystore file. Error message: ../wrongPath/localhost.truststore.p12"));
            assertTrue(message.contains("Possible solution: Verify correct path to the keystore. Change owner or permission to the keystore file."));
        }
    }

    @Nested
    class GivenSafKeyring {
        @Test
        void whenNoPathFound_thenStoresNotInitializeExceptionIsThrown() {
            String[] args = {"--keystore", "safkeyring:////userId/keyRing",
                "--truststore", "safkeyring:////userId/keyRing",
                "--keypasswd", "password",
                "--keyalias", "localhost"};
            ApimlConf conf = new ApimlConf();
            new CommandLine(conf).parseArgs(args);
            StoresNotInitializeException e = assertThrows(StoresNotInitializeException.class, () -> new Stores(conf));
            assertEquals("unknown protocol: safkeyring",e.getMessage());
        }

        @Test
        void whenWrongFormat_thenStoresNotInitializeExceptionIsThrown() {
            String[] args = {"--keystore", "keyring://userId/keyRing",
                "--truststore", "safkeyring:////userId/keyRing",
                "--keypasswd", "password",
                "--keyalias", "localhost"};
            ApimlConf conf = new ApimlConf();
            new CommandLine(conf).parseArgs(args);
            StoresNotInitializeException e = assertThrows(StoresNotInitializeException.class, () -> new Stores(conf));
            assertTrue(
                e.getMessage().replace('\\', '/')
                    .contains("Error while loading keystore file. Error message:")
            );
        }

        @Test
        void whenTruststoreIsKeyring_thenKeyRingUrlIsUsed() {
            String[] args = {"--keystore", "../keystore/localhost/localhost.keystore.p12",
                "--truststore", "safkeyring://userId/keyRing",
                "--keypasswd", "password",
                "--keyalias", "localhost"};
            ApimlConf conf = new ApimlConf();
            new CommandLine(conf).parseArgs(args);
            StoresNotInitializeException e = assertThrows(StoresNotInitializeException.class, () -> new Stores(conf));
            assertEquals("unknown protocol: safkeyring", e.getMessage());
        }

        @Test
        void givenValidKeyringUri_whenProtocolHandlerNotAvailable_thenThrowsMalformedURLException() {
            assertThrows(MalformedURLException.class, () -> KeyringUtils.keyRingUrl("safkeyring://userId/keyRing"));
        }

        @Test
        void whenKeyRingUrlCalledWithInvalidFormat_thenThrowsStoresNotInitializeException() {
            StoresNotInitializeException e = assertThrows(StoresNotInitializeException.class,
                () -> KeyringUtils.keyRingUrl("notakeyring://bad/format/extra"));
            assertTrue(e.getMessage().contains("Incorrect key ring format"));
        }

        @Test
        void givenIsKeyring_whenValidSafkeyringUri_thenReturnsTrue() {
            assertTrue(KeyringUtils.isKeyring("safkeyring://userId/keyRing"));
            assertTrue(KeyringUtils.isKeyring("safkeyring:////userId/keyRing"));
            assertTrue(KeyringUtils.isKeyring("safkeyringce://userId/keyRing"));
        }

        @Test
        void givenIsKeyring_whenInvalidUri_thenReturnsFalse() {
            assertFalse(KeyringUtils.isKeyring(null));
            assertFalse(KeyringUtils.isKeyring(""));
            assertFalse(KeyringUtils.isKeyring("/path/to/file.p12"));
            assertFalse(KeyringUtils.isKeyring("keyring://userId/keyRing"));
        }

        @Test
        void givenFormatKeyringUrl_whenUriHasExtraSlashes_thenNormalized() {
            assertEquals("safkeyring://userId/keyRing", KeyringUtils.formatKeyringUrl("safkeyring:////userId/keyRing"));
            assertEquals("safkeyring://userId/keyRing", KeyringUtils.formatKeyringUrl("safkeyring://userId/keyRing"));
        }

        @Test
        void givenFormatKeyringUrl_whenNull_thenReturnsNull() {
            assertNull(KeyringUtils.formatKeyringUrl(null));
        }
    }

}
