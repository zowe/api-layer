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

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SslOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.caching.service.Storage;
import org.zowe.apiml.caching.service.redis.RedisOperator;
import org.zowe.apiml.caching.service.redis.RedisStorage;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.log.ApimlLogger;

import java.io.File;
import java.time.Duration;

@Configuration
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "caching.storage.mode", havingValue = "redis")
public class RedisConfiguration {
    private final RedisConfig redisConfig;


    @Bean
    public Storage redis(MessageService messageService) {
        log.info("Using redis configuration {}", redisConfig);

        RedisURI redisUri = createRedisUri();
        RedisClient redisClient = createRedisClient();

        return new RedisStorage(new RedisOperator(redisClient, redisUri, ApimlLogger.of(RedisOperator.class, messageService)));
    }

    /**
     * Package protected for unit testing.
     */
    RedisURI createRedisUri() {
        RedisURI.Builder uriBuilder = RedisURI.builder()
            .withSsl(redisConfig.usesSsl())
            .withAuthentication(redisConfig.getUsername(), redisConfig.getPassword())
            .withTimeout(Duration.ofSeconds(redisConfig.getTimeout()));

        if (redisConfig.usesSentinel()) {
            RedisConfig.Sentinel sentinelConfig = redisConfig.getSentinel();
            uriBuilder.withSentinelMasterId(sentinelConfig.getMasterInstance());

            for (RedisConfig.Sentinel.SentinelNode sentinelNode : sentinelConfig.getNodes()) {
                uriBuilder.withSentinel(sentinelNode.getHost(), sentinelNode.getPort(), sentinelNode.getPassword());
            }

        } else {
            uriBuilder
                .withHost(redisConfig.getHost())
                .withPort(redisConfig.getPort());
        }

        return uriBuilder.build();
    }

    /**
     * Package protected for unit testing.
     */
    RedisClient createRedisClient() {
        RedisClient redisClient = RedisClient.create();

        if (redisConfig.usesSsl()) {
            RedisConfig.SslConfig sslConfig = redisConfig.getSsl();

            SslOptions sslOptions = SslOptions.builder()
                .jdkSslProvider()
                .keystore(new File(sslConfig.getKeyStore()), sslConfig.getKeyStorePassword().toCharArray())
                .truststore(new File(sslConfig.getTrustStore()), sslConfig.getTrustStorePassword())
                .build();

            redisClient.setOptions(ClientOptions.builder().sslOptions(sslOptions).build());
        }

        return redisClient;
    }
}
