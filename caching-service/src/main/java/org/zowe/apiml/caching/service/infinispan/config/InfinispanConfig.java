/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.service.infinispan.config;

import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.lock.EmbeddedClusteredLockManagerFactory;
import org.infinispan.lock.api.ClusteredLock;
import org.infinispan.lock.api.ClusteredLockManager;
import org.infinispan.manager.DefaultCacheManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.zowe.apiml.caching.service.Storage;
import org.zowe.apiml.caching.service.infinispan.exception.InfinispanConfigException;
import org.zowe.apiml.caching.service.infinispan.storage.InfinispanStorage;

import java.io.IOException;
import java.io.InputStream;

@Configuration
@ConfigurationProperties(value = "caching.storage.infinispan")
@ConditionalOnProperty(name = "caching.storage.mode", havingValue = "infinispan")
public class InfinispanConfig {

    @Value("${caching.storage.infinispan.initialHosts}")
    private String initialHosts;
    @Value("${caching.storage.infinispan.persistence.dataLocation}")
    private String dataLocation;
    @Value("${caching.storage.infinispan.persistence.indexLocation:index}")
    private String indexLocation;
    @Value("${server.ssl.keyStoreType}")
    private String keyStoreType;
    @Value("${server.ssl.keyStore}")
    private String keyStore;
    @Value("${server.ssl.keyStorePassword}")
    private String keyStorePass;
    @Value("${jgroups.bind.port}")
    private String port;
    @Value("${jgroups.bind.address}")
    private String address;


    @Bean
    DefaultCacheManager cacheManager(ResourceLoader resourceLoader) {
        System.setProperty("jgroups.tcpping.initial_hosts", initialHosts);
        System.setProperty("jgroups.bind.port", port);
        System.setProperty("jgroups.bind.address", address);
        System.setProperty("server.ssl.keyStoreType", keyStoreType);
        System.setProperty("server.ssl.keyStore", keyStore);
        System.setProperty("server.ssl.keyStorePassword", keyStorePass);
        ConfigurationBuilderHolder holder;

        try (InputStream configurationStream = resourceLoader.getResource(
            "classpath:infinispan.xml").getInputStream()) {
            holder = new ParserRegistry().parse(configurationStream, null, MediaType.APPLICATION_XML);
        } catch (IOException e) {
            throw new InfinispanConfigException("Can't read configuration file", e);
        }

        DefaultCacheManager cacheManager = new DefaultCacheManager(holder, true);

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.clustering().cacheMode(CacheMode.REPL_SYNC)
            .encoding().mediaType("application/x-jboss-marshalling");

        builder.persistence().passivation(true)
            .addSoftIndexFileStore()
            .shared(false)
            .dataLocation(dataLocation).indexLocation(indexLocation);
        cacheManager.administration()
            .withFlags(CacheContainerAdmin.AdminFlag.VOLATILE)
            .getOrCreateCache("zoweCache", builder.build());
        cacheManager.administration()
            .withFlags(CacheContainerAdmin.AdminFlag.VOLATILE)
            .getOrCreateCache("zoweInvalidatedTokenCache", builder.build());
        return cacheManager;
    }

    @Bean
    public ClusteredLock lock(DefaultCacheManager cacheManager) {
        ClusteredLockManager clm = EmbeddedClusteredLockManagerFactory.from(cacheManager);
        clm.defineLock("zoweInvalidatedTokenLock");
        return clm.get("zoweInvalidatedTokenLock");
    }


    @Bean
    public Storage storage(DefaultCacheManager cacheManager, ClusteredLock clusteredLock) {
        return new InfinispanStorage(cacheManager.getCache("zoweCache"), cacheManager.getCache("zoweInvalidatedTokenCache"), clusteredLock);
    }

}
