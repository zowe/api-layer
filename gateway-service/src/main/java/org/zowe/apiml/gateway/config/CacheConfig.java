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

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.cache.jcache.JCacheManagerFactoryBean;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.zowe.apiml.cache.CompositeKeyGenerator;
import org.zowe.apiml.cache.CompositeKeyGeneratorWithoutLast;
import org.zowe.apiml.util.CacheUtils;

import java.io.IOException;

/**
 * Spring configuration to use EhCache. This context is using from application and also from tests.
 */
@Slf4j
@EnableCaching
@Configuration
public class CacheConfig {

    public static final String COMPOSITE_KEY_GENERATOR = "compositeKeyGenerator";
    public static final String COMPOSITE_KEY_GENERATOR_WITHOUT_LAST = "compositeKeyGeneratorWithoutLast";

    private static final String EHCACHE_STORAGE_ENV_PARAM_NAME = "ehcache.disk.store.dir";
    private static final String APIML_CACHE_STORAGE_LOCATION_ENV_PARAM_NAME = "apiml.cache.storage.location";

    @Value("${apiml.caching.enabled:true}")
    private boolean cacheEnabled;

    @PostConstruct
    public void afterPropertiesSet() {
        if (cacheEnabled) {
            if (System.getProperty(EHCACHE_STORAGE_ENV_PARAM_NAME) == null) {
                String location = System.getProperty(APIML_CACHE_STORAGE_LOCATION_ENV_PARAM_NAME);
                if (location == null) location = System.getProperty("user.dir");

                System.setProperty(EHCACHE_STORAGE_ENV_PARAM_NAME, location);
            }
        } else {
            log.warn("Gateway Service is running in NoOp Cache mode. Do not use in production. " +
               "To enable caching set configuration property apiml.caching.enabled to true."
            );
        }
    }

    @Bean
    @ConditionalOnProperty(value = "apiml.caching.enabled", havingValue = "true", matchIfMissing = true)
    public JCacheManagerFactoryBean cacheManagerFactoryBean() throws IOException {
        JCacheManagerFactoryBean jCacheManagerFactoryBean = new JCacheManagerFactoryBean();
        jCacheManagerFactoryBean.setCacheManagerUri(new ClassPathResource("ehcache.xml").getURI());
        return jCacheManagerFactoryBean;
    }

    @Bean("cacheManager")
    @ConditionalOnProperty(value = "apiml.caching.enabled", havingValue = "true", matchIfMissing = true)
    public CacheManager cacheManager() throws IOException {
        final JCacheCacheManager jCacheCacheManager = new JCacheCacheManager();
        jCacheCacheManager.setCacheManager(cacheManagerFactoryBean().getObject());
        return jCacheCacheManager;
    }

    @ConditionalOnProperty(value = "apiml.caching.enabled", havingValue = "false")
    @Bean("cacheManager")
    public CacheManager cacheManagerNoOp() {
        return new NoOpCacheManager();
    }

    @Bean(CacheConfig.COMPOSITE_KEY_GENERATOR)
    public KeyGenerator getCompositeKeyGenerator() {
        return new CompositeKeyGenerator();
    }

    @Bean(CacheConfig.COMPOSITE_KEY_GENERATOR_WITHOUT_LAST)
    public KeyGenerator getCompositeKeyGeneratorWithoutLast() {
        return new CompositeKeyGeneratorWithoutLast();
    }

    @Bean
    public CacheUtils cacheUtils() {
        return new CacheUtils();
    }

}
