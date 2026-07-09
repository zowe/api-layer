/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.eurekaservice.client.config;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.product.logging.LogMessageTracker;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiMediationServiceConfigTest {

    private final LogMessageTracker logTracker = new LogMessageTracker(ApiMediationServiceConfig.class);

    @BeforeEach
    void setup() {
        logTracker.startTracking();
    }

    @AfterEach
    void cleanUp() {
        logTracker.stopTracking();
    }

    @Test
    void givenNoSsl_whenFormatKeyringUrlAndSetPasswordIfNotPresent_thenNoExceptionIsThrown() {
        ApiMediationServiceConfig config = new ApiMediationServiceConfig();

        assertDoesNotThrow(config::formatKeyringUrlAndSetPasswordIfNotPresent);
    }

    @Test
    void givenKeyStoreContainsSafkeyring_whenSetKeyringPasswordIfNotPresent_thenPasswordIsFormatToUrlAndSetPassword() {
        Ssl ssl = new Ssl();
        ssl.setKeyStore("safkeyring://ring/label");
        ApiMediationServiceConfig config = new ApiMediationServiceConfig();
        config.setSsl(ssl);

        config.formatKeyringUrlAndSetPasswordIfNotPresent();

        assertArrayEquals("password".toCharArray(), ssl.getKeyStorePassword());
        assertArrayEquals("password".toCharArray(), ssl.getKeyPassword());
        assertTrue(logTracker.contains("Keystore is a z/OS keyring. Formatting the url and defaulting the key and keystore password if needed", Level.DEBUG));
    }

    @Test
    void givenKeyStoreDoesNotContainSafkeyring_whenFormatKeyringPasswordIfNotPresent_thenUrlAndSetPasswordIsUnchanged() {
        Ssl ssl = new Ssl();
        ssl.setKeyStore("keystore/localhost/localhost.keystore.p12");
        ApiMediationServiceConfig config = new ApiMediationServiceConfig();
        config.setSsl(ssl);

        config.formatKeyringUrlAndSetPasswordIfNotPresent();

        assertNull(ssl.getKeyStorePassword());
        assertEquals(0, logTracker.countEvents());
    }

    @Test
    void givenKeyStorePasswordAlreadySet_whenFormatKeyringPasswordIfNotPresent_thenUrlAndSetPasswordIsUnchanged() {
        Ssl ssl = new Ssl();
        ssl.setKeyStore("safkeyring://ring/label");
        ssl.setKeyStorePassword("existing".toCharArray());
        ssl.setKeyPassword("existing".toCharArray());
        ApiMediationServiceConfig config = new ApiMediationServiceConfig();
        config.setSsl(ssl);

        config.formatKeyringUrlAndSetPasswordIfNotPresent();

        assertArrayEquals("existing".toCharArray(), ssl.getKeyStorePassword());
        assertArrayEquals("existing".toCharArray(), ssl.getKeyPassword());
        assertTrue(logTracker.contains("Keystore is a z/OS keyring. Formatting the url and defaulting the key and keystore password if needed", Level.DEBUG));
    }

    @Test
    void givenKeyStoreContainsSafkeyringJce_whenFormatKeyringUrlAndSetPasswordIfNotPresent_thenUrlIsFormattedAndPasswordIsSet() {
        Ssl ssl = new Ssl();
        ssl.setKeyStore("safkeyringjce:////ring/label");
        ApiMediationServiceConfig config = new ApiMediationServiceConfig();
        config.setSsl(ssl);

        config.formatKeyringUrlAndSetPasswordIfNotPresent();

        assertEquals("safkeyringjce://ring/label", ssl.getKeyStore());
        assertArrayEquals("password".toCharArray(), ssl.getKeyStorePassword());
        assertArrayEquals("password".toCharArray(), ssl.getKeyPassword());
        assertTrue(logTracker.contains("Keystore is a z/OS keyring. Formatting the url and defaulting the key and keystore password if needed", Level.DEBUG));
    }

    @Test
    void givenTrustStoreContainsSafkeyring_whenSetKeyringPasswordIfNotPresent_thenPasswordIsFormatToUrlAndSetPassword() {
        Ssl ssl = new Ssl();
        ssl.setTrustStore("safkeyring://ring/label");
        ApiMediationServiceConfig config = new ApiMediationServiceConfig();
        config.setSsl(ssl);

        config.formatKeyringUrlAndSetPasswordIfNotPresent();

        assertArrayEquals("password".toCharArray(), ssl.getTrustStorePassword());
        assertTrue(logTracker.contains("Truststore is a z/OS keyring. Formatting the url and defaulting the password if needed", Level.DEBUG));
    }

    @Test
    void givenTrustStoreDoesNotContainSafkeyring_whenFormatKeyringPasswordIfNotPresent_thenUrlAndSetPasswordIsUnchangedAndNotLogged() {
        Ssl ssl = new Ssl();
        ssl.setTrustStore("truststore/localhost/localhost.truststore.p12");
        ApiMediationServiceConfig config = new ApiMediationServiceConfig();
        config.setSsl(ssl);

        config.formatKeyringUrlAndSetPasswordIfNotPresent();

        assertNull(ssl.getTrustStorePassword());
        assertEquals(0, logTracker.countEvents());
    }

    @Test
    void givenTrustStorePasswordAlreadySet_whenFormatKeyringPasswordIfNotPresent_thenUrlAndSetPasswordIsUnchanged() {
        Ssl ssl = new Ssl();
        ssl.setTrustStore("safkeyring://ring/label");
        ssl.setTrustStorePassword("existing".toCharArray());
        ApiMediationServiceConfig config = new ApiMediationServiceConfig();
        config.setSsl(ssl);

        config.formatKeyringUrlAndSetPasswordIfNotPresent();

        assertArrayEquals("existing".toCharArray(), ssl.getTrustStorePassword());
        assertTrue(logTracker.contains("Truststore is a z/OS keyring. Formatting the url and defaulting the password if needed", Level.DEBUG));
    }

    @Test
    void givenTrustStoreContainsSafkeyringJce_whenFormatKeyringUrlAndSetPasswordIfNotPresent_thenUrlIsFormattedAndPasswordIsSet() {
        Ssl ssl = new Ssl();
        ssl.setTrustStore("safkeyringjce:////ring/label");
        ApiMediationServiceConfig config = new ApiMediationServiceConfig();
        config.setSsl(ssl);

        config.formatKeyringUrlAndSetPasswordIfNotPresent();

        assertEquals("safkeyringjce://ring/label", ssl.getTrustStore());
        assertArrayEquals("password".toCharArray(), ssl.getTrustStorePassword());
        assertTrue(logTracker.contains("Truststore is a z/OS keyring. Formatting the url and defaulting the password if needed", Level.DEBUG));
    }

}
