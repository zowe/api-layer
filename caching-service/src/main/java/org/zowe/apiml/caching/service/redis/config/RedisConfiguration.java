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

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RedisConfiguration {
    private final RedisConfig redisConfig;

    @ConditionalOnProperty(name = "caching.storage.mode", havingValue = "redis")
    @Bean
    public Storage redis(MessageService messageService) {
        log.info("Using redis configuration {}", redisConfig);
        // TODO what if sentinel and master have different passwords? Is this possible?

        // TODO RedisConfig parse the application.yml and here detect if using sentinel or not. Then make URI based on RedisConfig.redisConfig
        //RedisURI redisUri = new RedisURI(config.getHostIP(), config.getPort(), Duration.ofSeconds(config.getTimeout()));
        RedisURI redisUri = RedisURI.Builder.sentinel("127.0.0.1", "redismaster").build();
        redisUri.setUsername(redisConfig.getUsername());
        redisUri.setPassword(redisConfig.getPassword().toCharArray());

        return new RedisStorage(new RedisOperator(redisUri, ApimlLogger.of(RedisOperator.class, messageService)));
    }
}
