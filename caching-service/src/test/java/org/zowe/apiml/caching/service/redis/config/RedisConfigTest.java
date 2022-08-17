/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.service.redis.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.caching.config.GeneralConfig;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RedisConfigTest {
    private static final int PORT = 1234;
    private static final String HOST = "host";
    private static final String USER = "user";
    private static final String PASSWORD = "pass";

    private RedisConfig underTest;

    @BeforeEach
    void setup() {
        GeneralConfig generalConfig = mock(GeneralConfig.class);
        underTest = new RedisConfig(generalConfig);
    }

    @Nested
    class WhenCheckForSentinel {
        @Test
        void givenNullSentinel_thenReturnFalse() {
            assertFalse(underTest.usesSentinel());
        }

        @Test
        void givenSentinelNotEnabled_thenReturnFalse() {
            underTest.setSentinel(new RedisConfig.Sentinel());
            assertFalse(underTest.usesSentinel());
        }

        @Test
        void givenValidSentinel_thenReturnTrue() {
            RedisConfig.Sentinel sentinel = new RedisConfig.Sentinel();
            sentinel.setEnabled(true);

            underTest.setSentinel(sentinel);
            assertTrue(underTest.usesSentinel());
        }
    }

    @Nested
    class WhenParseUri {
        @Nested
        class WhenParseMasterUriCredentials {
            @Test
            void givenUsernameAndPassword_thenUseBoth() {
                String uri = String.format("%s:%s@%s", USER, PASSWORD, HOST);
                underTest.setMasterNodeUri(uri);
                underTest.init();

                assertEquals(USER, underTest.getUsername());
                assertEquals(PASSWORD, underTest.getPassword());
            }

            @Test
            void givenOnlyPassword_thenUseGivenPasswordAndDefaultUsername() {
                String uri = String.format("%s@%s", PASSWORD, HOST);
                underTest.setMasterNodeUri(uri);
                underTest.init();

                assertEquals("default", underTest.getUsername());
                assertEquals(PASSWORD, underTest.getPassword());
            }

            @Test
            void givenNoUsernameOrPassword_thenUseDefaultUsernameAndNoPassword() {
                underTest.setMasterNodeUri(HOST);
                underTest.init();

                assertEquals("default", underTest.getUsername());
                assertEquals("", underTest.getPassword());
            }
        }

        @Nested
        class WhenParseSentinelUriCredentials {
            @Test
            void givenPassword_thenSetPassword() {
                String uri = String.format("%s@%s", PASSWORD, HOST);
                RedisConfig.Sentinel.SentinelNode node = new RedisConfig.Sentinel.SentinelNode(uri);
                assertEquals(PASSWORD, node.getPassword());
            }

            @Test
            void givenNoPassword_thenNoPassword() {
                RedisConfig.Sentinel.SentinelNode node = new RedisConfig.Sentinel.SentinelNode(HOST);
                assertEquals("", node.getPassword());
            }
        }

        @Nested
        class WhenParsePort {
            @Test
            void givenPort_thenSetPort() {
                String uri = String.format("%s:%d", HOST, PORT);
                RedisConfig.Sentinel.SentinelNode node = new RedisConfig.Sentinel.SentinelNode(uri);
                assertEquals(PORT, node.getPort());
            }

            @Test
            void givenNoPort_thenUseDefaultPort() {
                RedisConfig.Sentinel.SentinelNode node = new RedisConfig.Sentinel.SentinelNode(HOST);
                assertEquals(6379, node.getPort());
            }
        }

        @Nested
        class WhenParseHost_ThenSetHost {

            @Test
            void givenUsernameAndPasswordAndNoPort() {
                String uri = String.format("%s:%s@%s", USER, PASSWORD, HOST);
                RedisConfig.Sentinel.SentinelNode node = new RedisConfig.Sentinel.SentinelNode(uri);
                assertEquals(HOST, node.getHost());
            }

            @Test
            void givenUsernameAndPasswordAndPort() {
                String uri = String.format("%s:%s@%s:%d", USER, PASSWORD, HOST, PORT);
                RedisConfig.Sentinel.SentinelNode node = new RedisConfig.Sentinel.SentinelNode(uri);
                assertEquals(HOST, node.getHost());
            }

            @Test
            void givenNoCredentialsAndPort() {
                String uri = String.format("%s:%d", HOST, PORT);
                RedisConfig.Sentinel.SentinelNode node = new RedisConfig.Sentinel.SentinelNode(uri);
                assertEquals(HOST, node.getHost());
            }

            @Test
            void givenNoCredentialsAndNoPort() {
                RedisConfig.Sentinel.SentinelNode node = new RedisConfig.Sentinel.SentinelNode(HOST);
                assertEquals(HOST, node.getHost());
            }
        }
    }

    @Nested
    class WhenCheckForSsl {
        @Test
        void givenNullSslConfig_thenReturnFalse() {
            assertFalse(underTest.usesSsl());
        }

        @Test
        void givenSslNotEnabled_thenReturnFalse() {
            RedisConfig.SslConfig sslConfig = new RedisConfig.SslConfig();
            sslConfig.setEnabled(false);
            underTest.setSsl(sslConfig);

            assertFalse(underTest.usesSsl());
        }

        @Test
        void givenEnabledSsl_thenReturnTrue() {
            underTest.setSsl(new RedisConfig.SslConfig());
            assertTrue(underTest.usesSsl());
        }
    }
}
