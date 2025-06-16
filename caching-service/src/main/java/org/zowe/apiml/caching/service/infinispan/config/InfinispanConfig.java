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

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
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
import java.nio.file.Paths;

import static org.zowe.apiml.security.SecurityUtils.formatKeyringUrl;
import static org.zowe.apiml.security.SecurityUtils.isKeyring;

@Configuration
@ConfigurationProperties(value = "caching.storage.infinispan")
@ConditionalOnProperty(name = "caching.storage.mode", havingValue = "infinispan")
public class InfinispanConfig {

    private static final String KEYRING_PASSWORD = "password";

    @Value("${caching.storage.infinispan.initialHosts}")
    private String initialHosts;
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
    @Value("${jgroups.keyExchange.port:7601}")
    private String keyExchangePort;
    @Value("${jgroups.tcp.diag.enabled:false}")
    private String tcpDiagEnabled;

    @PostConstruct
    void updateKeyring() {
        if (isKeyring(keyStore)) {
            keyStore = formatKeyringUrl(keyStore);
            if (StringUtils.isBlank(keyStorePass)) keyStorePass = KEYRING_PASSWORD;
        }
    }

    static String getRootFolder() {
        // using getenv().get is because of system compatibility (see non-case sensitive on Windows)
        String instanceId = System.getenv().get("ZWE_haInstance_id");
        if (StringUtils.isBlank(instanceId)) {
            instanceId = "localhost";
        }

        String workspaceFolder = System.getenv().get("ZWE_zowe_workspaceDirectory");
        if (StringUtils.isBlank(workspaceFolder)) {
            return Paths.get("caching-service", instanceId).toString();
        } else {
            return Paths.get(workspaceFolder, "caching-service", instanceId).toString();
        }
    }

    @Bean(destroyMethod = "stop")
    DefaultCacheManager cacheManager(ResourceLoader resourceLoader) {
        System.setProperty("jgroups.tcpping.initial_hosts", initialHosts);
        System.setProperty("jgroups.bind.port", port);
        System.setProperty("jgroups.bind.address", address);
        System.setProperty("jgroups.keyExchange.port", keyExchangePort);
        System.setProperty("server.ssl.keyStoreType", keyStoreType);
        System.setProperty("server.ssl.keyStore", keyStore);
        System.setProperty("server.ssl.keyStorePassword", keyStorePass);
        System.setProperty("jgroups.tcp.diag.enabled", String.valueOf(Boolean.parseBoolean(tcpDiagEnabled)));
        ConfigurationBuilderHolder holder;

        try (InputStream configurationStream = resourceLoader.getResource(
            "classpath:infinispan.xml").getInputStream()) {
            holder = new ParserRegistry().parse(configurationStream, MediaType.APPLICATION_XML);
        } catch (IOException e) {
            throw new InfinispanConfigException("Can't read configuration file", e);
        }
        holder.getGlobalConfigurationBuilder().globalState().persistentLocation(getRootFolder()).enable();
        holder.newConfigurationBuilder("default").persistence().passivation(true).addSoftIndexFileStore()
            .shared(false);

        DefaultCacheManager cacheManager = new DefaultCacheManager(holder, true);

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.clustering().cacheMode(CacheMode.REPL_SYNC)
            .encoding().mediaType("application/x-jboss-marshalling");

        builder.persistence().passivation(true)
            .addSoftIndexFileStore()
            .shared(false);
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
