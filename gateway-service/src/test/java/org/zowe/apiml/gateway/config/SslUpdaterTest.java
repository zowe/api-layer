/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.server.Ssl;

import static org.junit.jupiter.api.Assertions.*;

class SslUpdaterTest {

    private Ssl ssl;
    private ServerProperties serverProperties;

    @BeforeEach
    void setUp() {
        ssl = new Ssl();
        serverProperties = new ServerProperties();
        serverProperties.setSsl(ssl);
    }

    @Nested
    class NoKeyring {

        @Test
        void unsupportedClass() {
            assertDoesNotThrow(() -> new SslUpdater().postProcessAfterInitialization(new Object(), "bean"));
        }

        @Test
        void dontUpdate() {
            ssl.setKeyStore("/path1");
            ssl.setTrustStore("/path/2");

            new SslUpdater().postProcessAfterInitialization(ssl, "bean");

            assertEquals("/path1", ssl.getKeyStore());
            assertEquals("/path/2", ssl.getTrustStore());
            assertNull(ssl.getKeyStorePassword());
            assertNull(ssl.getKeyPassword());
            assertNull(ssl.getTrustStorePassword());
        }
    }

    @Nested
    class WithKeyring {

        @BeforeEach
        void setUp() {
            ssl.setKeyStore("safkeyring:////userId/ringId1");
            ssl.setTrustStore("safkeyringjce:///userId/ringId2");
        }

        @Test
        void givenNoPassword_thenSetThem() {
            new SslUpdater().postProcessAfterInitialization(serverProperties, "bean");
            assertEquals("password", ssl.getKeyStorePassword());
            assertEquals("password", ssl.getKeyPassword());
            assertEquals("password", ssl.getTrustStorePassword());
        }

        @Test
        void givenInvalidPath_thenFixThem() {
            new SslUpdater().postProcessAfterInitialization(serverProperties, "bean");
            assertEquals("safkeyring://userId/ringId1", ssl.getKeyStore());
            assertEquals("safkeyringjce://userId/ringId2", ssl.getTrustStore());
        }

    }

}