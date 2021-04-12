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

import io.lettuce.core.RedisURI;
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

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RedisConfiguration {
    private final RedisConfig redisConfig;

    @ConditionalOnProperty(name = "caching.storage.mode", havingValue = "redis")
    @Bean
    public Storage redis(MessageService messageService) {
        log.info("Using redis configuration {}", redisConfig);
        RedisURI redisUri = createRedisUri();

        return new RedisStorage(new RedisOperator(redisUri, ApimlLogger.of(RedisOperator.class, messageService)));
    }

    /**
     * Package protected for unit testing.
     */
    RedisURI createRedisUri() {
        RedisURI.Builder uriBuilder = RedisURI.builder()
            .withAuthentication(redisConfig.getUsername(), redisConfig.getPassword())
            .withTimeout(Duration.ofSeconds(redisConfig.getTimeout()));

        if (redisConfig.usesSentinel()) {
            RedisConfig.Sentinel sentinelConfig = redisConfig.getSentinel();
            uriBuilder.withSentinelMasterId(sentinelConfig.getMaster());

            for (RedisConfig.Sentinel.SentinelNode sentinelNode : sentinelConfig.getNodes()) {
                uriBuilder.withSentinel(sentinelNode.getIp(), sentinelNode.getPort(), sentinelNode.getPassword());
            }

        } else {
            uriBuilder
                .withHost(redisConfig.getMasterIP())
                .withPort(redisConfig.getMasterPort());
        }

        return uriBuilder.build();
    }
}
