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
import lombok.extern.slf4j.Slf4j;
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
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.partitionhandling.AvailabilityException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.zowe.apiml.cache.Storage;
import org.zowe.apiml.cache.StorageException;
import org.zowe.apiml.caching.service.Messages;
import org.zowe.apiml.caching.service.infinispan.exception.InfinispanConfigException;
import org.zowe.apiml.caching.service.infinispan.storage.InfinispanStorage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.zowe.apiml.security.SecurityUtils.formatKeyringUrl;
import static org.zowe.apiml.security.SecurityUtils.isKeyring;

@Slf4j
@Configuration
@ConfigurationProperties(value = "caching.storage.infinispan")
@ConditionalOnProperty(name = "caching.storage.mode", havingValue = "infinispan")
public class InfinispanConfig implements InitializingBean {

    private static final String KEYRING_PASSWORD = "password";
    private static final String SERVER_SSL_KEY_STORE_TYPE = "server.ssl.keyStoreType";
    private static final String SERVER_SSL_KEY_STORE = "server.ssl.keyStore";
    private static final String SERVER_SSL_KEY_STORE_PASSWORD = "server.ssl.keyStorePassword";

    private static final String ZWE_HAINSTANCE_ID = "ZWE_haInstance_id";
    private static final String LOCK_ZOWE_INVALIDATED = "zoweInvalidatedTokenLock";
    private static final String CACHE_ZOWE = "zoweCache";
    private static final String CACHE_ZOWE_INVALIDATED_TOKEN = "zoweInvalidatedTokenCache";

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

    @Value("${server.attlsServer.enabled:false}")
    private boolean isServerAttlsEnabled;

    private AtomicReference<ClusteredLock> zoweInvalidatedTokenLock = new AtomicReference<>();

    @Override
    public void afterPropertiesSet() {
        updateKeyring();
    }

    @PostConstruct
    void updateKeyring() {
        if (isKeyring(keyStore)) {
            keyStore = formatKeyringUrl(keyStore);
            if (StringUtils.isBlank(keyStorePass)) keyStorePass = KEYRING_PASSWORD;
        }
    }

    static String getRootFolder() {
        // using getenv().get is because of system compatibility (see non-case sensitive on Windows)
        String instanceId = System.getenv().get(ZWE_HAINSTANCE_ID);
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
    synchronized LazyCacheManager cacheManager(ResourceLoader resourceLoader) {
        return new LazyCacheManager(() -> createCacheManager(resourceLoader));
    }

    DefaultCacheManager createCacheManager(ResourceLoader resourceLoader) {
        System.setProperty("jgroups.tcpping.initial_hosts", initialHosts);
        System.setProperty("jgroups.bind.port", port);
        System.setProperty("jgroups.bind.address", address);
        System.setProperty("jgroups.keyExchange.port", keyExchangePort);
        System.setProperty("jgroups.tcp.diag.enabled", String.valueOf(Boolean.parseBoolean(tcpDiagEnabled)));

        var oldKeyStoreType = Optional.ofNullable(System.getProperty(SERVER_SSL_KEY_STORE_TYPE));
        var oldKeyStore = Optional.ofNullable(System.getProperty(SERVER_SSL_KEY_STORE));
        var oldKeyStorePassword = Optional.ofNullable(System.getProperty(SERVER_SSL_KEY_STORE_PASSWORD));

        if (!isServerAttlsEnabled) {
            System.setProperty(SERVER_SSL_KEY_STORE_TYPE, keyStoreType);
            System.setProperty(SERVER_SSL_KEY_STORE, keyStore);
            System.setProperty(SERVER_SSL_KEY_STORE_PASSWORD, keyStorePass);
        }

        ConfigurationBuilderHolder holder;

        var infinispanConfigFile = isServerAttlsEnabled ? "infinispan-attls.xml" : "infinispan.xml";
        try (InputStream configurationStream = resourceLoader.getResource("classpath:" + infinispanConfigFile).getInputStream()) {
            holder = new ParserRegistry().parse(configurationStream, MediaType.APPLICATION_XML);
        } catch (IOException e) {
            throw new InfinispanConfigException("Can't read configuration file", e);
        }
        holder.getGlobalConfigurationBuilder().globalState().persistentLocation(getRootFolder()).enable();
        holder.newConfigurationBuilder("default")
            .persistence()
            .addSoftIndexFileStore()
            .clustering().cacheMode(CacheMode.DIST_SYNC);
        holder.getGlobalConfigurationBuilder().defaultCacheName("default");

        DefaultCacheManager cacheManager = new DefaultCacheManager(holder, true);

        try {
            ConfigurationBuilder builder = new ConfigurationBuilder();
            builder
                .encoding().mediaType(MediaType.APPLICATION_JBOSS_MARSHALLING_TYPE)
                .persistence().addSoftIndexFileStore().clustering()
                .clustering().cacheMode(CacheMode.DIST_SYNC);

            var caches = Arrays.asList(CACHE_ZOWE, CACHE_ZOWE_INVALIDATED_TOKEN, "zosmfAuthenticationEndpoint", "invalidatedJwtTokens", "validationJwtToken", "zosmfInfo", "zosmfJwtEndpoint", "trustedCertificates", "parseOIDCToken", "validationOIDCToken");
            caches.forEach(cacheName -> {
                cacheManager.administration()
                    .withFlags(CacheContainerAdmin.AdminFlag.VOLATILE)
                    .getOrCreateCache(cacheName, builder.build());
            });

            oldKeyStoreType.ifPresent(kst -> System.setProperty(SERVER_SSL_KEY_STORE_TYPE, kst));
            oldKeyStore.ifPresent(ks -> System.setProperty(SERVER_SSL_KEY_STORE, ks));
            oldKeyStorePassword.ifPresent(p -> System.setProperty(SERVER_SSL_KEY_STORE_PASSWORD, p));
        } catch (Exception e) {
            log.warn("Error during creation of default cache manager", e);
            try {
                cacheManager.stop();
            } catch (Exception e2) {
                log.debug("Cannot stop cache manager", e2);
            }
            return null;
        }

        return cacheManager;
    }

    private ClusteredLock lock(CacheContainer cacheManager) {
        ClusteredLock lock = zoweInvalidatedTokenLock.get();
        if (lock != null) {
            return lock;
        }

        try {
            synchronized (zoweInvalidatedTokenLock) {
                lock = zoweInvalidatedTokenLock.get();
                if (lock == null && cacheManager instanceof LazyCacheManager lazyCacheManager) {
                    ClusteredLockManager clm = EmbeddedClusteredLockManagerFactory.from(lazyCacheManager.getOriginal());
                    // it can throw AvailabilityException
                    clm.defineLock(LOCK_ZOWE_INVALIDATED);
                    lock = clm.get(LOCK_ZOWE_INVALIDATED);
                }
                zoweInvalidatedTokenLock.set(lock);
            }
            return lock;
        } catch (AvailabilityException ae) {
            log.debug("Cannot obtain lock", ae);
            throw new StorageException(Messages.CACHE_NOT_AVAILABLE.getKey(), Messages.CACHE_NOT_AVAILABLE.getStatus(), ae.getMessage());
        }
    }

    @Bean
    public Storage storage(CacheContainer cacheManager) {
        return new InfinispanStorage(
            cacheManager.getCache(CACHE_ZOWE),
            cacheManager.getCache(CACHE_ZOWE_INVALIDATED_TOKEN),
            () -> lock(cacheManager)
        );
    }

}
