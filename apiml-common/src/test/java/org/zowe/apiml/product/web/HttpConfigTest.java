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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class HttpConfigTest {

    @Nested
    class KeyringFormatAndPasswordUpdate {

        @Test
        void whenKeyringHasWrongFormatAndMissingPasswords_thenFixIt() {
            HttpConfig httpConfig = new HttpConfig();
            ReflectionTestUtils.setField(httpConfig, "keyStore", "safkeyring:///userId/ringId1");
            ReflectionTestUtils.setField(httpConfig, "trustStore", "safkeyring:////userId/ringId2");

            httpConfig.updateStorePaths();

            assertEquals("safkeyring://userId/ringId1", ReflectionTestUtils.getField(httpConfig, "keyStore"));
            assertEquals("safkeyring://userId/ringId2", ReflectionTestUtils.getField(httpConfig, "trustStore"));
            assertArrayEquals("password".toCharArray(), (char[]) ReflectionTestUtils.getField(httpConfig, "keyStorePassword"));
            assertArrayEquals("password".toCharArray(), (char[]) ReflectionTestUtils.getField(httpConfig, "trustStorePassword"));
        }

        @Test
        void whenKeystore_thenDoNothing() {
            HttpConfig httpConfig = new HttpConfig();
            ReflectionTestUtils.setField(httpConfig, "keyStore", "/path1");
            ReflectionTestUtils.setField(httpConfig, "trustStore", "/path2");

            httpConfig.updateStorePaths();

            assertEquals("/path1", ReflectionTestUtils.getField(httpConfig, "keyStore"));
            assertEquals("/path2", ReflectionTestUtils.getField(httpConfig, "trustStore"));
            assertNull(ReflectionTestUtils.getField(httpConfig, "keyStorePassword"));
            assertNull(ReflectionTestUtils.getField(httpConfig, "trustStorePassword"));
        }

    }

}