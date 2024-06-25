/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.core.config.DefaultConfiguration;
import org.ehcache.impl.config.persistence.DefaultPersistenceConfiguration;
import org.ehcache.impl.config.store.disk.OffHeapDiskStoreConfiguration;
import org.ehcache.impl.config.store.disk.OffHeapDiskStoreProviderConfiguration;
import org.ehcache.impl.copy.IdentityCopier;
import org.ehcache.impl.copy.SerializingCopier;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.cache.CompositeKeyGenerator;
import org.zowe.apiml.cache.CompositeKeyGeneratorWithoutLast;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.util.CacheUtils;
import org.zowe.apiml.zaas.cache.CachingServiceClient;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;

import javax.cache.Caching;
import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;

/**
 * Spring configuration to use EhCache. This context is using from application and also from tests.
 */
@Slf4j
@EnableCaching
@Configuration
@RequiredArgsConstructor
public class CacheConfig {

    public static final String COMPOSITE_KEY_GENERATOR = "compositeKeyGenerator";
    public static final String COMPOSITE_KEY_GENERATOR_WITHOUT_LAST = "compositeKeyGeneratorWithoutLast";

    private static final String EHCACHE_STORAGE_ENV_PARAM_NAME = "ehcache.disk.store.dir";
    private static final String APIML_CACHE_STORAGE_LOCATION_ENV_PARAM_NAME = "apiml.cache.storage.location";

    @Value("${apiml.caching.enabled:true}")
    private boolean cacheEnabled;

    @Value("${apiml.cache.storage.location:./ehcache}")
    private String cacheDirectory;

    @PostConstruct
    public void afterPropertiesSet() {
        if (cacheEnabled) {
            if (System.getProperty(EHCACHE_STORAGE_ENV_PARAM_NAME) == null) {
                String location = System.getProperty(APIML_CACHE_STORAGE_LOCATION_ENV_PARAM_NAME);
                if (location == null) location = System.getProperty("user.dir");

                System.setProperty(EHCACHE_STORAGE_ENV_PARAM_NAME, location);
            }
        } else {
            log.warn("ZAAS is running in NoOp Cache mode. Do not use in production. " +
                "To enable caching set configuration property apiml.caching.enabled to true."
            );
        }
    }

    @Bean("cacheManager")
    @ConditionalOnProperty(value = "apiml.caching.enabled", havingValue = "true", matchIfMissing = true)
    public CacheManager cacheManager() {
        var caches = new HashMap<String, CacheConfiguration<?, ?>>();

        var invalidatedJwtTokensConf = CacheConfigurationBuilder.newCacheConfigurationBuilder(
                String.class, Boolean.class, ResourcePoolsBuilder.newResourcePoolsBuilder().disk(10, MemoryUnit.MB).heap(1, MemoryUnit.MB)
            ).withService(new OffHeapDiskStoreConfiguration("pool1", 1, 1))
            .withKeyCopier(IdentityCopier.identityCopier())
            .withValueCopier(IdentityCopier.identityCopier())
            .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofDays(1)))
            .build();

        caches.put("invalidatedJwtTokens", invalidatedJwtTokensConf);

        var validationJwtTokenConf = CacheConfigurationBuilder.newCacheConfigurationBuilder(
                String.class, TokenAuthentication.class, ResourcePoolsBuilder.newResourcePoolsBuilder().heap(1000, EntryUnit.ENTRIES)
            )
            .withKeyCopier(IdentityCopier.identityCopier())
            .withValueCopier(SerializingCopier.asCopierClass())
            .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofDays(1))).build();
        caches.put("validationJwtToken", validationJwtTokenConf);

        var zosmfInfoConf = CacheConfigurationBuilder.newCacheConfigurationBuilder(
                String.class, String.class, ResourcePoolsBuilder.newResourcePoolsBuilder().heap(10, EntryUnit.ENTRIES)
            )
            .withKeyCopier(IdentityCopier.identityCopier())
            .withValueCopier(IdentityCopier.identityCopier())
            .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofHours(1))).build();
        caches.put("zosmfInfo", zosmfInfoConf);

        var zosmfAuthenticationEndpointConf = CacheConfigurationBuilder.newCacheConfigurationBuilder(
                String.class, Boolean.class, ResourcePoolsBuilder.newResourcePoolsBuilder().heap(10, EntryUnit.ENTRIES)
            )
            .withKeyCopier(IdentityCopier.identityCopier())
            .withValueCopier(IdentityCopier.identityCopier())
            .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofHours(1))).build();
        caches.put("zosmfAuthenticationEndpoint", zosmfAuthenticationEndpointConf);

        var zosmfJwtEndpointConf = CacheConfigurationBuilder.newCacheConfigurationBuilder(
                HttpHeaders.class, Boolean.class, ResourcePoolsBuilder.newResourcePoolsBuilder().heap(10, EntryUnit.ENTRIES)
            )
            .withKeyCopier(SerializingCopier.asCopierClass())
            .withValueCopier(IdentityCopier.identityCopier())
            .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofHours(1))).build();
        caches.put("zosmfJwtEndpoint", zosmfJwtEndpointConf);

        var validationOIDCTokenConf = CacheConfigurationBuilder.newCacheConfigurationBuilder(
                AuthSource.class, Boolean.class, ResourcePoolsBuilder.newResourcePoolsBuilder().heap(1000, EntryUnit.ENTRIES)
            )
            .withKeyCopier(IdentityCopier.identityCopier())
            .withValueCopier(IdentityCopier.identityCopier())
            .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(20))).build();
        caches.put("validationOIDCToken", validationOIDCTokenConf);

        var parseOIDCTokenConf = CacheConfigurationBuilder.newCacheConfigurationBuilder(
                AuthSource.class, AuthSource.Parsed.class, ResourcePoolsBuilder.newResourcePoolsBuilder().heap(1000, EntryUnit.ENTRIES)
            )
            .withKeyCopier(IdentityCopier.identityCopier())
            .withValueCopier(IdentityCopier.identityCopier())
            .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(20))).build();
        caches.put("parseOIDCToken", parseOIDCTokenConf);

        var trustedCertificatesConf = CacheConfigurationBuilder.newCacheConfigurationBuilder(
                String.class, List.class, ResourcePoolsBuilder.newResourcePoolsBuilder().heap(1000, EntryUnit.ENTRIES)
            )
            .withKeyCopier(IdentityCopier.identityCopier())
            .withValueCopier(IdentityCopier.identityCopier())
            .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofHours(1))).build();
        caches.put("trustedCertificates", trustedCertificatesConf);

        EhcacheCachingProvider provider = (EhcacheCachingProvider) Caching.getCachingProvider("org.ehcache.jsr107.EhcacheCachingProvider");
        var serviceProvider = new OffHeapDiskStoreProviderConfiguration("pool1");
        var localPersStore = new DefaultPersistenceConfiguration(new File(cacheDirectory));

        org.ehcache.config.Configuration configuration = new DefaultConfiguration(caches, provider.getDefaultClassLoader(), serviceProvider, localPersStore);

        var cacheManager = provider.getCacheManager(provider.getDefaultURI(), configuration);

        return new JCacheCacheManager(cacheManager);
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

    @Bean
    public CachingServiceClient cachingServiceClient(GatewayClient gatewayClient, @Qualifier("restTemplateWithKeystore") RestTemplate restTemplate) {
        return new CachingServiceClient(restTemplate, gatewayClient);
    }

}
