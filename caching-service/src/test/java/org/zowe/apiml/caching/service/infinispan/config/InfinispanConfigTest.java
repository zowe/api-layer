/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.service.infinispan.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InfinispanConfigTest {

    @Nested
    class Initialization {

        @Test
        void whenKeyringUrlIsInvalidAndMissingPassword_thenFixKeyringUrlAndSetPassword() {
            InfinispanConfig infinispanConfig = new InfinispanConfig();
            ReflectionTestUtils.setField(infinispanConfig, "keyStore", "safkeyringpce:///userId/ringId");
            infinispanConfig.updateKeyring();
            assertEquals("safkeyringpce://userId/ringId", ReflectionTestUtils.getField(infinispanConfig, "keyStore"));
            assertEquals("password", ReflectionTestUtils.getField(infinispanConfig, "keyStorePass"));
        }

        @Test
        void whenKeyringUrlIsInvalidAndSetPassword_thenFixKeyringUrl() {
            InfinispanConfig infinispanConfig = new InfinispanConfig();
            ReflectionTestUtils.setField(infinispanConfig, "keyStore", "safkeyring:///userId/ringId");
            ReflectionTestUtils.setField(infinispanConfig, "keyStorePass", "pswd");
            infinispanConfig.updateKeyring();
            assertEquals("safkeyring://userId/ringId", ReflectionTestUtils.getField(infinispanConfig, "keyStore"));
            assertEquals("pswd", ReflectionTestUtils.getField(infinispanConfig, "keyStorePass"));
        }

        @Test
        void whenKeystore_thenDontUpdate() {
            InfinispanConfig infinispanConfig = new InfinispanConfig();
            ReflectionTestUtils.setField(infinispanConfig, "keyStore", "/path");
            ReflectionTestUtils.setField(infinispanConfig, "keyStorePass", "pass");
            infinispanConfig.updateKeyring();
            assertEquals("/path", ReflectionTestUtils.getField(infinispanConfig, "keyStore"));
            assertEquals("pass", ReflectionTestUtils.getField(infinispanConfig, "keyStorePass"));
        }

    }

}