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

import lombok.RequiredArgsConstructor;
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
public class RedisConfiguration {
    private final RedisConfig redisConfig;

    @ConditionalOnProperty(name = "caching.storage.mode", havingValue = "redis")
    @Bean
    public Storage redis(MessageService messageService) {
        // TODO lettuce use sentinel address as redis uri to connect, this will return connection to master - can reuse hostip, port, etc
        // TODO will need RedisConfig to specify if using sentinel or not and have a different way to connect via a new RedisConnection class
        // TODO what if sentinel and master have different passwords? Is this possible?

        // TODO should probably use spring data's LettuceConnectionFactory
        // and other spring integrations: https://medium.com/trendyol-tech/high-availability-with-redis-sentinel-and-spring-lettuce-client-9da40525fc82
        // another link: https://michaelcgood.com/spring-data-redis-sentinel/
        // though, if I can just connect to sentinel and then get the master node, this is ok.
        // but, what if that sentinel goes down? Then what? That is the problem here... we will see.
        // I think we can just do an array of sentinel nodes and do with sentinel for each one
        return new RedisStorage(new RedisOperator(redisConfig, ApimlLogger.of(RedisOperator.class, messageService)));
    }
}
