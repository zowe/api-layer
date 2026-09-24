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

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
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
import org.infinispan.lock.exception.ClusteredLockException;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.partitionhandling.AvailabilityException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.zowe.apiml.cache.PatRevocationStore;
import org.zowe.apiml.cache.Storage;
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
    public static final String CACHE_ZOWE = "zoweCache";

    /** @deprecated defined for previous-release peers only; nothing on this node takes it. */
    @Deprecated(since = "3.6.0") // scheduled for removal with the legacy read path
    private static final String LOCK_ZOWE_INVALIDATED = "zoweInvalidatedTokenLock";

    /**
     * The pre-cutover revocation store: one cache entry per <em>map</em>, holding the whole map as its value.
     * Frozen and read-only from this release on - it is only consulted for personal access tokens issued
     * before the cutover, and it is removed together with the legacy read path.
     *
     * @deprecated superseded by {@link #CACHE_ZOWE_INVALIDATED_TOKEN_ITEM}.
     */
    @Deprecated(since = "3.6.0") // scheduled for removal with the legacy read path
    public static final String CACHE_ZOWE_INVALIDATED_TOKEN = "zoweInvalidatedTokenCache";

    /**
     * The revocation store: one cache entry per item, with native per-entry expiration.
     * <p>
     * Deliberately a new cache name rather than a reuse of {@link #CACHE_ZOWE_INVALIDATED_TOKEN}: the two
     * hold different value types, and a previous-release node scanning a cache that mixed them would fail
     * deserialization and reject every personal access token. An older node is simply unaware of this name.
     */
    public static final String CACHE_ZOWE_INVALIDATED_TOKEN_ITEM = "zoweInvalidatedTokenItemCache";

    private static final long SMALL_CACHE_SIZE = 10;
    private static final long BIG_CACHE_SIZE = 1000;

    /**
     * Ceiling for any entry of the revocation store. A personal access token lives at most 90 days and a
     * revocation rule stops being relevant after the same period, so nothing in that cache can legitimately
     * need longer.
     */
    private static final Duration REVOCATION_MAX_TTL = Duration.ofDays(PatRevocationStore.RULE_RETENTION_DAYS);

    @Value("${caching.storage.infinispan.initialHosts:}")
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

    @Value("${caching.storage.infinispan.revocationStore.maxCount:100000}")
    private long revocationStoreMaxCount;

    @Value("${caching.storage.infinispan.revocationStore.sizeWarningThreshold:50000}")
    private long revocationStoreSizeWarningThreshold;

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

    /**
     * Same replicated, persisted shape as {@link #getDistributedCacheConfig()}, plus an entry-count bound.
     * Bounding is only safe - and only meaningful - with the per-item layout: the soft-index file store is
     * write-through (passivation is off by default), so evicting an entry drops only the in-memory copy and a
     * later read falls back to disk. Expiration itself is per entry, set on the write.
     */
    private ConfigurationBuilder getRevocationCacheConfig() {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder
            .encoding().mediaType(MediaType.APPLICATION_JBOSS_MARSHALLING_TYPE)
            .memory()
            .maxCount(revocationStoreMaxCount)
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
        caches.put(CACHE_ZOWE_INVALIDATED_TOKEN_ITEM, getRevocationCacheConfig());
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

    /**
     * Nothing <em>takes</em> the lock any more. It existed solely to serialise the whole-map
     * read-modify-write of the previous layout; with one entry per item every write is a single atomic
     * {@code put} and every removal a compare-and-remove, so nothing needs cluster-wide mutual exclusion.
     * <p>
     * It is still <em>defined</em>, for one release only. {@code defineLock} creates the internal replicated
     * {@code org.infinispan.LOCKS} cache, previous-release nodes in the same cluster still take the lock, and
     * a cluster where only some members define that internal cache is a configuration we would otherwise
     * have to prove safe rather than simply avoid. Removed in the release that drops the legacy read path,
     * together with the {@code infinispan-clustered-lock} dependency.
     */
    @Deprecated(since = "3.6.0") // scheduled for removal with the legacy read path
    private void defineLegacyLock(CacheContainer cacheManager) {
        EmbeddedCacheManager cm = (cacheManager instanceof LazyCacheManager lazyCacheManager)
            ? lazyCacheManager.getOriginal() : (EmbeddedCacheManager) cacheManager;
        try {
            EmbeddedClusteredLockManagerFactory.from(cm).defineLock(LOCK_ZOWE_INVALIDATED);
        } catch (AvailabilityException | ClusteredLockException e) {
            // Nothing on this node needs it, so this is not fatal here - it only matters to a
            // previous-release peer, which defines the lock itself anyway.
            log.debug("Cannot define the legacy clustered lock", e);
        }
    }

    @Bean
    public Storage storage(DefaultCacheManager cacheManager, ObjectProvider<MeterRegistry> meterRegistry) {
        defineLegacyLock(cacheManager);
        var storage = new InfinispanStorage(
            cacheManager,
            REVOCATION_MAX_TTL.toSeconds(),
            revocationStoreSizeWarningThreshold
        );
        meterRegistry.ifAvailable(registry -> Gauge
            .builder("apiml.caching.revocationStore.size", storage, InfinispanStorage::getLastObservedRevocationStoreSize)
            .description("Entries in the personal access token revocation store, as of the last sample taken on write")
            .register(registry));
        return storage;
    }

}
