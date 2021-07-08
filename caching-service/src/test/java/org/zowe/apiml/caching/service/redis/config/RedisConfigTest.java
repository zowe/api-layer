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
        void givenValidSentinel_thenReturnTrue() {
            underTest.setSentinel(new RedisConfig.Sentinel());
            assertTrue(underTest.usesSentinel());
        }
    }

    @Nested
    class WhenParseUri {
        @Nested
        class WhenParseMasterUriCredentials {
            @Test
            void givenUsernameAndPassword_thenUseBoth() {
                String uri = "user:pass@host";
                underTest.setMasterNodeUri(uri);
                underTest.init();

                assertEquals("user", underTest.getUsername());
                assertEquals("pass", underTest.getPassword());
            }

            @Test
            void givenOnlyUsername_thenUseGivenUsernameAndNoPassword() {
                String uri = "user@host";
                underTest.setMasterNodeUri(uri);
                underTest.init();

                assertEquals("user", underTest.getUsername());
                assertEquals("", underTest.getPassword());
            }

            @Test
            void givenNoUsernameOrPassword_thenUseDefaultUsernameAndNoPassword() {
                String uri = "host";
                underTest.setMasterNodeUri(uri);
                underTest.init();

                assertEquals("default", underTest.getUsername());
                assertEquals("", underTest.getPassword());
            }
        }

        @Nested
        class WhenParseSentinelUriCredentials {
            @Test
            void givenPassword_thenSetPassword() {
                String uri = "default:pass@host";
                RedisConfig.Sentinel.SentinelNode node = new RedisConfig.Sentinel.SentinelNode(uri);
                assertEquals("pass", node.getPassword());
            }

            @Test
            void givenNoPassword_thenNoPassword() {
                RedisConfig.Sentinel.SentinelNode node = new RedisConfig.Sentinel.SentinelNode("host");
                assertEquals("", node.getPassword());
            }
        }

        @Nested
        class WhenParsePort {
            @Test
            void givenPort_thenSetPort() {
                String uri = "host:1234";
                RedisConfig.Sentinel.SentinelNode node = new RedisConfig.Sentinel.SentinelNode(uri);
                assertEquals(1234, node.getPort());
            }

            @Test
            void givenNoPort_thenUseDefaultPort() {
                RedisConfig.Sentinel.SentinelNode node = new RedisConfig.Sentinel.SentinelNode("host");
                assertEquals(6379, node.getPort());
            }
        }

        @Nested
        class WhenParseHost_ThenSetHost {
            // TODO value source
            @Test
            void givenUsernameAndPasswordAndNoPort() {
                String uri = "user:pass@host";
                RedisConfig.Sentinel.SentinelNode node = new RedisConfig.Sentinel.SentinelNode(uri);
                assertEquals("host", node.getHost());
            }

            @Test
            void givenUsernameAndPasswordAndPort() {
                String uri = "user:pass@host:1234";
                RedisConfig.Sentinel.SentinelNode node = new RedisConfig.Sentinel.SentinelNode(uri);
                assertEquals("host", node.getHost());
            }

            @Test
            void givenNoCredentialsAndPort() {
                String uri = "host:1234";
                RedisConfig.Sentinel.SentinelNode node = new RedisConfig.Sentinel.SentinelNode(uri);
                assertEquals("host", node.getHost());
            }

            @Test
            void givenNoCredentialsAndNoPort() {
                String uri = "host";
                RedisConfig.Sentinel.SentinelNode node = new RedisConfig.Sentinel.SentinelNode(uri);
                assertEquals("host", node.getHost());
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
            underTest.setSsl(new RedisConfig.SslConfig());
            assertFalse(underTest.usesSsl());
        }

        @Test
        void givenEnabledSsl_thenReturnTrue() {
            RedisConfig.SslConfig sslConfig = new RedisConfig.SslConfig();
            sslConfig.setEnabled(true);
            underTest.setSsl(sslConfig);

            assertTrue(underTest.usesSsl());
        }
    }
}
