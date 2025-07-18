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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.server.Ssl;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HttpConfigTest {

    @Mock
    ApplicationContext context;

    @InjectMocks
    HttpConfig httpConfig;

    @Nested
    class KeyringFormatAndPasswordUpdate {

        @BeforeEach
        void setup() {
            ServerProperties properties = new ServerProperties();
            properties.setSsl(new Ssl());
            when(context.getBean(ServerProperties.class)).thenReturn(properties);
        }

        @Test
        void whenKeyringHasWrongFormatAndMissingPasswords_thenFixIt() {
            ReflectionTestUtils.setField(httpConfig, "keyStorePath", "safkeyring:///userId/ringId1");
            ReflectionTestUtils.setField(httpConfig, "trustStorePath", "safkeyring:////userId/ringId2");

            httpConfig.updateStorePaths();

            assertEquals("safkeyring://userId/ringId1", ReflectionTestUtils.getField(httpConfig, "keyStorePath"));
            assertEquals("safkeyring://userId/ringId2", ReflectionTestUtils.getField(httpConfig, "trustStorePath"));
            assertArrayEquals("password".toCharArray(), (char[]) ReflectionTestUtils.getField(httpConfig, "keyStorePassword"));
            assertArrayEquals("password".toCharArray(), (char[]) ReflectionTestUtils.getField(httpConfig, "trustStorePassword"));
        }

        @Test
        void whenKeystore_thenDoNothing() {
            ReflectionTestUtils.setField(httpConfig, "keyStorePath", "/path1");
            ReflectionTestUtils.setField(httpConfig, "trustStorePath", "/path2");

            httpConfig.updateStorePaths();

            assertEquals("/path1", ReflectionTestUtils.getField(httpConfig, "keyStorePath"));
            assertEquals("/path2", ReflectionTestUtils.getField(httpConfig, "trustStorePath"));
            assertNull(ReflectionTestUtils.getField(httpConfig, "keyStorePassword"));
            assertNull(ReflectionTestUtils.getField(httpConfig, "trustStorePassword"));
        }

    }

}
