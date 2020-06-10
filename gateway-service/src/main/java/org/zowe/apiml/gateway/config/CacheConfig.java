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

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.zowe.apiml.cache.CompositeKeyGenerator;
import org.zowe.apiml.cache.CompositeKeyGeneratorWithoutLast;
import org.zowe.apiml.util.CacheUtils;

import javax.annotation.PostConstruct;

/**
 * Spring configuration to use EhCache. This context is using from application and also from tests.
 */
@EnableCaching
@Configuration
public class CacheConfig {

    public static final String COMPOSITE_KEY_GENERATOR = "compositeKeyGenerator";
    public static final String COMPOSITE_KEY_GENERATOR_WITHOUT_LAST = "compositeKeyGeneratorWithoutLast";

    private static final String EHCACHE_STORAGE_ENV_PARAM_NAME = "ehcache.disk.store.dir";
    private static final String APIML_CACHE_STORAGE_LOCATION_ENV_PARAM_NAME = "apiml.cache.storage.location";

    @PostConstruct
    public void afterPropertiesSet() {
        if (System.getProperty(EHCACHE_STORAGE_ENV_PARAM_NAME) == null) {
            String location = System.getProperty(APIML_CACHE_STORAGE_LOCATION_ENV_PARAM_NAME);
            if (location == null) location = System.getProperty("user.dir");

            System.setProperty(EHCACHE_STORAGE_ENV_PARAM_NAME, location);
        }
    }

    @Bean
    public CacheManager cacheManager() {
        return new EhCacheCacheManager(ehCacheCacheManager().getObject());
    }

    @Bean
    public EhCacheManagerFactoryBean ehCacheCacheManager() {
        EhCacheManagerFactoryBean cmfb = new EhCacheManagerFactoryBean();
        cmfb.setConfigLocation(new ClassPathResource("ehcache.xml"));
        cmfb.setShared(true);
        return cmfb;
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
