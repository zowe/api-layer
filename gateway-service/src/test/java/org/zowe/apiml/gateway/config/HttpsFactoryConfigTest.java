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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.Ssl;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class HttpsFactoryConfigTest {

    @Nested
    @SpringBootTest
    @ComponentScan(basePackages = "org.zowe.apiml.gateway")
    class KeyringFormatAndPasswordUpdate {

        @Mock
        ApplicationContext context;

        @InjectMocks
        HttpsFactoryConfig httpsFactoryConfig;

        @BeforeEach
        void setup() {
            ServerProperties properties = new ServerProperties();
            properties.setSsl(new Ssl());
            when(context.getBean(ServerProperties.class)).thenReturn(properties);
        }

        @Test
        void whenKeyringHasWrongFormatAndMissingPasswords_thenFixIt() {
            ReflectionTestUtils.setField(httpsFactoryConfig, "keyStorePath", "safkeyring:///userId/ringId1");
            ReflectionTestUtils.setField(httpsFactoryConfig, "trustStorePath", "safkeyring:////userId/ringId2");
            httpsFactoryConfig.updateConfigParameters();

            assertThat(ReflectionTestUtils.getField(httpsFactoryConfig, "keyStorePath")).isEqualTo("safkeyring://userId/ringId1");
            assertThat(ReflectionTestUtils.getField(httpsFactoryConfig, "trustStorePath")).isEqualTo("safkeyring://userId/ringId2");
            assertThat((char[]) ReflectionTestUtils.getField(httpsFactoryConfig, "keyStorePassword")).isEqualTo("password".toCharArray());
            assertThat((char[]) ReflectionTestUtils.getField(httpsFactoryConfig, "trustStorePassword")).isEqualTo("password".toCharArray());
        }

        @Test
        void whenKeystore_thenDoNothing() {
            ReflectionTestUtils.setField(httpsFactoryConfig, "keyStorePath", "/path1");
            ReflectionTestUtils.setField(httpsFactoryConfig, "trustStorePath", "/path2");
            httpsFactoryConfig.updateConfigParameters();

            assertThat(ReflectionTestUtils.getField(httpsFactoryConfig, "keyStorePath")).isEqualTo("/path1");
            assertThat(ReflectionTestUtils.getField(httpsFactoryConfig, "trustStorePath")).isEqualTo("/path2");
            assertThat(ReflectionTestUtils.getField(httpsFactoryConfig, "keyStorePassword")).isNull();
            assertThat(ReflectionTestUtils.getField(httpsFactoryConfig, "trustStorePassword")).isNull();
        }

    }

}
