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
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.lock.EmbeddedClusteredLockManagerFactory;
import org.infinispan.lock.api.ClusteredLock;
import org.infinispan.lock.api.ClusteredLockManager;
import org.infinispan.lock.exception.ClusteredLockException;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
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
import org.zowe.apiml.caching.service.infinispan.ApimlSslKeyExchange;
import org.zowe.apiml.caching.service.infinispan.exception.InfinispanConfigException;
import org.zowe.apiml.caching.service.infinispan.storage.InfinispanStorage;
import org.zowe.apiml.config.ApplicationInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.zowe.apiml.security.SecurityUtils.formatKeyringUrl;
import static org.zowe.apiml.security.SecurityUtils.isKeyring;

@Slf4j
@Configuration
@ConfigurationProperties(value = "caching.storage.infinispan")
@ConditionalOnProperty(name = "caching.storage.mode", havingValue = "infinispan")
public class InfinispanConfig implements InitializingBean {

    private static final String KEYRING_PASSWORD = "password";

    private static final String ZWE_HAINSTANCE_ID = "ZWE_haInstance_id";
    private static final String LOCK_ZOWE_INVALIDATED = "zoweInvalidatedTokenLock";
    public static final String CACHE_ZOWE = "zoweCache";
    public static final String CACHE_ZOWE_INVALIDATED_TOKEN = "zoweInvalidatedTokenCache";
    private static final long SMALL_CACHE_SIZE = 10;
    private static final long BIG_CACHE_SIZE = 1000;

    @Value("${caching.storage.infinispan.initialHosts}")
    private String initialHosts;

    @Value("${server.ssl.keyStoreType}")
    private String keyStoreType;

    @Value("${server.ssl.keyStore}")
    private String keyStore;

    @Value("${server.ssl.keyStorePassword}")
    private String keyStorePass;

    @Value("${server.ssl.trustStoreType}")
    private String trustStoreType;

    @Value("${server.ssl.trustStore}")
    private String trustStore;

    @Value("${server.ssl.trustStorePassword}")
    private String trustStorePass;

    @Value("${jgroups.bind.port}")
    private String port;

    @Value("${jgroups.bind.address}")
    private String address;

    @Value("${jgroups.keyExchange.socketTimeout:5000}")
    private String keyExchangeSocketTimeout;

    @Value("${jgroups.keyExchange.port:7601}")
    private String keyExchangePort;

    @Value("${jgroups.tcp.diag.enabled:false}")
    private String tcpDiagEnabled;

    @Value("${jgroups.tcpping.num_discovery_runs:20}")
    private int discoveryRuns;

    @Value("${attlsEnabledOnInfinispanTest:${server.attlsServer.enabled:false}}")
    private boolean isServerAttlsEnabled;

    @Value("${caching.storage.infinispan.distributedSyncTimeoutSecs:360}")
    private int distributedSyncTimeout;

    @Value("${caching.storage.infinispan.numSegments:256}")
    private int numSegments;

    @Value("${apiml.service.hostname:localhost}")
    private String hostname;

    private final AtomicReference<ClusteredLock> zoweInvalidatedTokenLock = new AtomicReference<>();

    @Override
    public void afterPropertiesSet() {
        updateKeyring();
    }

    private String getInitialHosts() {
        if (StringUtils.isNotEmpty(initialHosts)) {
            return initialHosts;
        }

        Pattern haHostname = Pattern.compile("^ZWE_haInstances_\\w+_hostname$");
        initialHosts = System.getenv().entrySet().stream()
            .filter(e -> haHostname.matcher(e.getKey()).matches())
            .map(Map.Entry::getValue)
            .map(h -> String.format("%s[%s]", h, port))
            .collect(Collectors.joining(","));

        if (StringUtils.isBlank(initialHosts)) {
            initialHosts = String.format("%s[%s]", hostname, port);
        }

        return initialHosts;
    }

    @PostConstruct
    void updateKeyring() {
        if (isKeyring(keyStore)) {
            keyStore = formatKeyringUrl(keyStore);
            if (StringUtils.isBlank(keyStorePass)) keyStorePass = KEYRING_PASSWORD;
        }
        if (isKeyring(trustStore)) {
            trustStore = formatKeyringUrl(trustStore);
            if (StringUtils.isBlank(trustStorePass)) trustStorePass = KEYRING_PASSWORD;
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

    public String getInfinispanConfigFile() {
        return isServerAttlsEnabled ? "infinispan-attls.xml" : "infinispan.xml";
    }

    private String loadInfinispanConfigFile(ResourceLoader resourceLoader) {
        String fileName = getInfinispanConfigFile();
        try (var inputStream = resourceLoader.getResource("classpath:" + fileName).getInputStream()) {
            String config = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            config = config.replace("jgroup:SSL_KEY_EXCHANGE", ApimlSslKeyExchange.class.getCanonicalName());
            return config;
        } catch (IOException ioe) {
            throw new InfinispanConfigException("Can't read configuration file", ioe);
        }
    }

    private ConfigurationBuilderHolder getCacheManagerConfig(ResourceLoader resourceLoader) {
        String config = loadInfinispanConfigFile(resourceLoader);
        ConfigurationBuilderHolder holder = new ParserRegistry().parse(config, MediaType.APPLICATION_XML);
        holder.getGlobalConfigurationBuilder().globalState().persistentLocation(getRootFolder()).enable();
        holder.newConfigurationBuilder("default")
            .persistence()
            .addSoftIndexFileStore()
            .clustering()
            .cacheMode(CacheMode.REPL_SYNC)
            .hash().numSegments(numSegments);
        holder.getGlobalConfigurationBuilder().defaultCacheName("default");
        holder.getGlobalConfigurationBuilder().transport().stack("prod").distributedSyncTimeout(distributedSyncTimeout, TimeUnit.SECONDS);
        return holder;
    }

    private ConfigurationBuilder getDistributedCacheConfig() {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder
            .encoding().mediaType(MediaType.APPLICATION_JBOSS_MARSHALLING_TYPE)
            .persistence()
            .addSoftIndexFileStore()
            .clustering()
            .cacheMode(CacheMode.REPL_SYNC)
            .hash().numSegments(numSegments);
        return builder;
    }

    private ConfigurationBuilder getSimpleCacheConfig(long maxCount, Duration lifeSpan) {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder
            .encoding().mediaType(MediaType.APPLICATION_JBOSS_MARSHALLING_TYPE)
            .memory()
            .storage(StorageType.OFF_HEAP)
            .maxCount(maxCount)
            .simpleCache(true)
            .expiration()
            .lifespan(lifeSpan.toSeconds(), TimeUnit.SECONDS);
        return builder;
    }

    @Bean(destroyMethod = "stop")
    LazyCacheManager cacheManager(ResourceLoader resourceLoader, ApplicationInfo applicationInfo) {
        System.setProperty("jgroups.tcpping.initial_hosts", getInitialHosts());
        System.setProperty("jgroups.tcpping.num_discovery_runs", String.valueOf(discoveryRuns));
        System.setProperty("jgroups.bind.port", port);
        System.setProperty("jgroups.bind.address", address);
        System.setProperty("jgroups.keyExchange.socketTimeout", keyExchangeSocketTimeout);
        System.setProperty("jgroups.keyExchange.port", keyExchangePort);
        System.setProperty("jgroups.tcp.diag.enabled", String.valueOf(Boolean.parseBoolean(tcpDiagEnabled)));

        System.setProperty("infinispan.ssl.keyStoreType", keyStoreType);
        System.setProperty("infinispan.ssl.keyStore", keyStore);
        System.setProperty("infinispan.ssl.keyStorePassword", keyStorePass);

        System.setProperty("infinispan.ssl.trustStoreType", trustStoreType);
        System.setProperty("infinispan.ssl.trustStore", trustStore);
        System.setProperty("infinispan.ssl.trustStorePassword", trustStorePass);

        var caches = new HashMap<String, ConfigurationBuilder>();
        caches.put(CACHE_ZOWE, getDistributedCacheConfig());
        caches.put(CACHE_ZOWE_INVALIDATED_TOKEN, getDistributedCacheConfig());

        if (applicationInfo.isModulith()) {
            caches.put("invalidatedJwtTokens", getDistributedCacheConfig());

            // 1 minute to force zosmf tokens validation against zosmf for invalidated tokens
            caches.put("validatedJwtTokens", getSimpleCacheConfig(BIG_CACHE_SIZE, Duration.ofMinutes(1)));

            //Small local caches
            caches.put("zosmfAuthenticationEndpoint", getSimpleCacheConfig(SMALL_CACHE_SIZE, Duration.ofHours(1)));
            caches.put("zosmfInfo", getSimpleCacheConfig(SMALL_CACHE_SIZE, Duration.ofHours(1)));
            caches.put("zosmfJwtEndpoint", getSimpleCacheConfig(SMALL_CACHE_SIZE, Duration.ofHours(1)));

            //Big local caches
            caches.put("trustedCertificates", getSimpleCacheConfig(BIG_CACHE_SIZE, Duration.ofHours(1)));
            caches.put("parseOIDCToken", getSimpleCacheConfig(BIG_CACHE_SIZE, Duration.ofSeconds(20)));
            caches.put("validationOIDCToken", getSimpleCacheConfig(BIG_CACHE_SIZE, Duration.ofSeconds(20)));
        }

        return new LazyCacheManager(getCacheManagerConfig(resourceLoader), caches);
    }

    private ClusteredLock lock(CacheContainer cacheManager) {
        return zoweInvalidatedTokenLock.updateAndGet(prev -> {
            if (prev != null) {
                return prev;
            }

            EmbeddedCacheManager cm = (cacheManager instanceof LazyCacheManager lazyCacheManager) ? lazyCacheManager.getOriginal() : (EmbeddedCacheManager) cacheManager;
            try {
                ClusteredLockManager clm = EmbeddedClusteredLockManagerFactory.from(cm);
                clm.defineLock(LOCK_ZOWE_INVALIDATED); // it can throw AvailabilityException
                return clm.get(LOCK_ZOWE_INVALIDATED);
            } catch (AvailabilityException | ClusteredLockException e) {
                log.debug("Cannot obtain lock", e);
                throw new StorageException(Messages.CACHE_NOT_AVAILABLE.getKey(), Messages.CACHE_NOT_AVAILABLE.getStatus(), e.getMessage());
            }
        });
    }

    @Bean
    public Storage storage(DefaultCacheManager cacheManager) {
        return new InfinispanStorage(
            cacheManager,
            () -> lock(cacheManager)
        );
    }

}
