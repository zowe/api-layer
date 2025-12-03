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
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.partitionhandling.AvailabilityException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.zowe.apiml.caching.service.Messages;
import org.zowe.apiml.caching.service.Storage;
import org.zowe.apiml.caching.service.StorageException;
import org.zowe.apiml.caching.service.infinispan.exception.InfinispanConfigException;
import org.zowe.apiml.caching.service.infinispan.storage.InfinispanStorage;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
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

    @Value("${jgroups.keyExchange.port:7118}")
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
    synchronized DefaultCacheManager cacheManager(ResourceLoader resourceLoader) {
        System.setProperty("jgroups.tcpping.initial_hosts", initialHosts);
        System.setProperty("jgroups.bind.port", port);
        System.setProperty("jgroups.bind.address", address);
        System.setProperty("jgroups.keyExchange.port", keyExchangePort);
        System.setProperty("jgroups.tcp.diag.enabled", String.valueOf(Boolean.parseBoolean(tcpDiagEnabled)));

        Optional<String> oldKeyStoreType = Optional.ofNullable(System.getProperty("SERVER_SSL_KEY_STORE_TYPE"));
        Optional<String> oldKeyStore = Optional.ofNullable(System.getProperty("SERVER_SSL_KEY_STORE"));
        Optional<String> oldKeyStorePassword = Optional.ofNullable(System.getProperty("SERVER_SSL_KEY_STORE_PASSWORD"));

        if (!isServerAttlsEnabled) {
            System.setProperty(SERVER_SSL_KEY_STORE_TYPE, keyStoreType);
            System.setProperty(SERVER_SSL_KEY_STORE, keyStore);
            System.setProperty(SERVER_SSL_KEY_STORE_PASSWORD, keyStorePass);
        }

        ConfigurationBuilderHolder holder;
        String infinispanConfigFile = isServerAttlsEnabled ? "infinispan-attls.xml" : "infinispan.xml";

        try (InputStream configurationStream = resourceLoader.getResource("classpath:" + infinispanConfigFile).getInputStream()) {
            holder = new ParserRegistry().parse(configurationStream, null, MediaType.APPLICATION_XML);
        } catch (IOException e) {
            throw new InfinispanConfigException("Can't read configuration file", e);
        }
        holder.getGlobalConfigurationBuilder().globalState().persistentLocation(getRootFolder()).enable();
        holder.newConfigurationBuilder("default").persistence().passivation(true).addSoftIndexFileStore()
            .shared(false);

        DefaultCacheManager cacheManager = new DefaultCacheManager(holder, true);

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.clustering()
            .cacheMode(CacheMode.REPL_SYNC)
            .encoding()
            .mediaType("application/x-jboss-marshalling");

        builder.persistence()
            .passivation(true)
            .addSoftIndexFileStore()
            .shared(false)
            .dataLocation(dataLocation).indexLocation(indexLocation);

        List<String> caches = Arrays.asList("zoweCache", "zoweInvalidatedTokenCache");
        caches.forEach(cacheName -> cacheManager.administration()
            .withFlags(CacheContainerAdmin.AdminFlag.VOLATILE)
            .getOrCreateCache(cacheName, builder.build()));

        oldKeyStoreType.ifPresent(keystoreType -> System.setProperty(SERVER_SSL_KEY_STORE_TYPE, keystoreType));
        oldKeyStore.ifPresent(keystore -> System.setProperty(SERVER_SSL_KEY_STORE, keystore));
        oldKeyStorePassword.ifPresent(keystorePassword -> System.setProperty(SERVER_SSL_KEY_STORE_PASSWORD, keystorePassword));

        return cacheManager;
    }

    private ClusteredLock lock(DefaultCacheManager cacheManager) {
        ClusteredLock lock = zoweInvalidatedTokenLock.get();
        if (lock != null) {
            return lock;
        }

        try {
            synchronized (zoweInvalidatedTokenLock) {
                lock = zoweInvalidatedTokenLock.get();
                if (lock == null) {
                    ClusteredLockManager clm = EmbeddedClusteredLockManagerFactory.from(cacheManager);
                    // it can throw AvailabilityException
                    clm.defineLock("zoweInvalidatedTokenLock");
                    lock = clm.get("zoweInvalidatedTokenLock");
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
    public Storage storage(DefaultCacheManager cacheManager) {
        return new InfinispanStorage(
            cacheManager.getCache("zoweCache"),
            cacheManager.getCache("zoweInvalidatedTokenCache"),
            () -> lock(cacheManager)
        );
    }

}
