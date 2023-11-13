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

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.util.CacheUtils;

import javax.annotation.PostConstruct;

/**
 * Spring configuration to disable EhCache usage.
 */
@EnableCaching
@Configuration
@ConditionalOnProperty(value = "apiml.caching.enabled", havingValue = "false")
@Slf4j
public class NoOpCacheConfig {

    @PostConstruct
    public void init() {
        log.warn("Gateway Service is runnnig in NoOp Cache mode. Do not use in production.");
    }

    @Bean
    public CacheManager cacheManager() {
        return new NoOpCacheManager();
    }

    @Bean
    public CacheUtils cacheUtils() {
        return new CacheUtils();
    }
}
