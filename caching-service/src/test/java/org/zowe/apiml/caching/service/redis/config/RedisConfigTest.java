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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
