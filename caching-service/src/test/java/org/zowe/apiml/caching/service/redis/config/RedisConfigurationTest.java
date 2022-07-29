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

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SslOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisConfigurationTest {
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String MASTER_IP = "127.0.0.1";
    private static final int MASTER_PORT = 6379;
    private static final int TIMEOUT = 60;

    private RedisConfiguration underTest;
    private RedisConfig redisConfig;

    @BeforeEach
    void setUp() {
        redisConfig = mock(RedisConfig.class);
        when(redisConfig.getUsername()).thenReturn(USERNAME);
        when(redisConfig.getPassword()).thenReturn(PASSWORD);
        when(redisConfig.getHost()).thenReturn(MASTER_IP);
        when(redisConfig.getPort()).thenReturn(MASTER_PORT);
        when(redisConfig.getTimeout()).thenReturn(TIMEOUT);

        underTest = new RedisConfiguration(redisConfig);
    }

    @Nested
    class WhenCreatingRedisClient {
        @Test
        void givenTlsFalse_thenReturnStandardRedisClient() {
            when(redisConfig.usesSsl()).thenReturn(false);

            RedisClient actual = underTest.createRedisClient();
            assertThat(actual.getOptions().getSslOptions(), samePropertyValuesAs(SslOptions.create()));
        }
    }

    @Test
    void givenRedisConfig_whenCreatingUriWithMasterReplica_thenReturnRedisUriWithProperties() {
        when(redisConfig.usesSentinel()).thenReturn(false);
        RedisURI result = underTest.createRedisUri();

        assertThat(result.getUsername(), is(USERNAME));
        assertThat(result.getPassword(), is(PASSWORD.toCharArray()));
        assertThat(result.getHost(), is(MASTER_IP));
        assertThat(result.getPort(), is(MASTER_PORT));
        assertThat(result.getTimeout(), is(Duration.ofSeconds(TIMEOUT)));
    }

    @Nested
    class WhenCreatingUriWithSentinel {
        private static final String MASTER = "redismaster";

        private RedisConfig.Sentinel sentinelConfig;

        @BeforeEach
        void useSentinel() {
            sentinelConfig = mock(RedisConfig.Sentinel.class);
            when(sentinelConfig.getMasterInstance()).thenReturn(MASTER);

            when(redisConfig.usesSentinel()).thenReturn(true);
            when(redisConfig.getSentinel()).thenReturn(sentinelConfig);
        }

        @Test
        void givenNoNodes_thenThrowIllegalStateException() {
            when(sentinelConfig.getNodes()).thenReturn(Collections.emptyList());
            assertThrows(IllegalStateException.class, () -> underTest.createRedisUri());
        }

        @Test
        void givenSentinelNodes_thenReturnRedisUri() {
            String ip1 = "1.2.3.4";
            String ip2 = "5.6.7.8";
            int port1 = 6379;
            int port2 = 6380;
            String password1 = "password1";
            String password2 = "password2";

            RedisConfig.Sentinel.SentinelNode node1 = mock(RedisConfig.Sentinel.SentinelNode.class);
            when(node1.getHost()).thenReturn(ip1);
            when(node1.getPort()).thenReturn(port1);
            when(node1.getPassword()).thenReturn(password1);

            RedisConfig.Sentinel.SentinelNode node2 = mock(RedisConfig.Sentinel.SentinelNode.class);
            when(node2.getHost()).thenReturn(ip2);
            when(node2.getPort()).thenReturn(port2);
            when(node2.getPassword()).thenReturn(password2);

            List<RedisConfig.Sentinel.SentinelNode> nodesList = new ArrayList<>();
            nodesList.add(node1);
            nodesList.add(node2);
            when(sentinelConfig.getNodes()).thenReturn(nodesList);

            RedisURI result = underTest.createRedisUri();
            assertThat(result.getUsername(), is(USERNAME));
            assertThat(result.getPassword(), is(PASSWORD.toCharArray()));
            assertThat(result.getSentinelMasterId(), is(MASTER));

            List<RedisURI> sentinelUris = result.getSentinels();
            assertThat(sentinelUris, is(not(nullValue())));
            assertThat(sentinelUris.size(), is(2));

            RedisURI sentinel1 = sentinelUris.get(0);
            assertThat(sentinel1.getHost(), is(ip1));
            assertThat(sentinel1.getPort(), is(port1));
            assertThat(sentinel1.getPassword(), is(password1.toCharArray()));

            RedisURI sentinel2 = sentinelUris.get(1);
            assertThat(sentinel2.getHost(), is(ip2));
            assertThat(sentinel2.getPort(), is(port2));
            assertThat(sentinel2.getPassword(), is(password2.toCharArray()));
        }
    }
}
