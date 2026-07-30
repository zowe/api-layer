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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.commons.compiler.util.Producer;
import org.infinispan.Cache;
import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.commons.configuration.ClassAllowList;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.health.Health;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.*;
import org.infinispan.remoting.transport.Address;
import org.infinispan.stats.CacheContainerStats;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import javax.security.auth.Subject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class LazyCacheManager extends DefaultCacheManager {

    // how many minutes wait thread to finish initialization by other threads
    private static final int INIT_TIMEOUT_MINS = 1;

    private final AtomicReference<Producer<DefaultCacheManager>> cacheManager;
    private final CacheInitializer cacheInitializer;

    public LazyCacheManager(
        ConfigurationBuilderHolder cacheManagerConfig,
        Map<String, ConfigurationBuilder> caches
    ) {
        super(cacheManagerConfig, false);
        cacheInitializer = new CacheInitializer(cacheManagerConfig, caches);
        cacheManager = new AtomicReference<>(cacheInitializer::getDefaultCacheManager);
    }

    private DefaultCacheManager getCacheManager() {
        var container = cacheManager.get().produce();
        if (container == null) {
            throw new IllegalStateException("Cache container is not initialized yet");
        }
        return container;
    }

    public boolean isInitialized() {
        return cacheInitializer.isInitialized();
    }

    @Override
    public Configuration defineConfiguration(String cacheName, Configuration configuration) {
        return getCacheManager().defineConfiguration(cacheName, configuration);
    }

    @Override
    public Configuration defineConfiguration(String cacheName, String templateCacheName, Configuration configurationOverride) {
        return getCacheManager().defineConfiguration(cacheName, templateCacheName, configurationOverride);
    }

    @Override
    public void undefineConfiguration(String configurationName) {
        getCacheManager().undefineConfiguration(configurationName);
    }

    @Override
    public String getClusterName() {
        return getCacheManager().getClusterName();
    }

    @Override
    public List<Address> getMembers() {
        return getCacheManager().getMembers();
    }

    @Override
    public Address getAddress() {
        return getCacheManager().getAddress();
    }

    @Override
    public Address getCoordinator() {
        return getCacheManager().getCoordinator();
    }

    @Override
    public boolean isCoordinator() {
        return getCacheManager().isCoordinator();
    }

    @Override
    public ComponentStatus getStatus() {
        return getCacheManager().getStatus();
    }

    @Override
    public GlobalConfiguration getCacheManagerConfiguration() {
        return getCacheManager().getCacheManagerConfiguration();
    }

    @Override
    public Configuration getCacheConfiguration(String name) {
        return getCacheManager().getCacheConfiguration(name);
    }

    @Override
    public Configuration getDefaultCacheConfiguration() {
        return getCacheManager().getDefaultCacheConfiguration();
    }

    @Override
    public Set<String> getAccessibleCacheNames() {
        return getCacheManager().getAccessibleCacheNames();
    }

    @Override
    public boolean isRunning(String cacheName) {
        return getCacheManager().isRunning(cacheName);
    }

    @Override
    public boolean isDefaultRunning() {
        return getCacheManager().isDefaultRunning();
    }

    @Override
    public boolean cacheExists(String cacheName) {
        return getCacheManager().cacheExists(cacheName);
    }

    @Override
    public boolean cacheConfigurationExists(String name) {
        return getCacheManager().cacheConfigurationExists(name);
    }

    @Override
    public <K, V> Cache<K, V> getCache() {
        return getCacheManager().getCache();
    }

    @Override
    public <K, V> Cache<K, V> getCache(String cacheName) {
        return getCacheManager().getCache(cacheName);
    }

    @Override
    public <K, V> Cache<K, V> createCache(String name, Configuration configuration) {
        return getCacheManager().createCache(name, configuration);
    }

    @Override
    public <K, V> Cache<K, V> getCache(String cacheName, boolean createIfAbsent) {
        return getCacheManager().getCache(cacheName, createIfAbsent);
    }

    @Override
    public EmbeddedCacheManager startCaches(String... cacheNames) {
        return getCacheManager().startCaches(cacheNames);
    }

    @Override
    public void addCacheDependency(String from, String to) {
        getCacheManager().addCacheDependency(from, to);
    }

    @Override
    public CacheContainerStats getStats() {
        return getCacheManager().getStats();
    }

    @Override
    public Health getHealth() {
        return getCacheManager().getHealth();
    }

    @Override
    public CacheManagerInfo getCacheManagerInfo() {
        return getCacheManager().getCacheManagerInfo();
    }

    @Override
    public ClassAllowList getClassAllowList() {
        return getCacheManager().getClassAllowList();
    }

    @Override
    public Subject getSubject() {
        return getCacheManager().getSubject();
    }

    @Override
    public EmbeddedCacheManager withSubject(Subject subject) {
        return getCacheManager().withSubject(subject);
    }

    @Override
    public Set<String> getCacheNames() {
        return getCacheManager().getCacheNames();
    }

    @Override
    public void start() {
        getCacheManager().start();
    }

    @Override
    public void stop() {
        getCacheManager().stop();
    }

    public <T extends CacheContainer> T getOriginal() {
        return (T) getCacheManager();
    }

    @Override
    public void close() throws IOException {
        getCacheManager().close();
    }

    @Override
    public CompletionStage<Void> addListenerAsync(Object listener) {
        return getCacheManager().addListenerAsync(listener);
    }

    @Override
    public CompletionStage<Void> removeListenerAsync(Object listener) {
        return getCacheManager().removeListenerAsync(listener);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationStart() {
        cacheInitializer.onApplicationStart();
    }

    @RequiredArgsConstructor
    class CacheInitializer {

        private DefaultCacheManager underInit;

        private final ConfigurationBuilderHolder cacheManagerConfig;
        private final Map<String, ConfigurationBuilder> caches;

        private final Phaser threadCounter = new Phaser();

        // avoids retrying the global-state reset forever if it doesn't fix the underlying issue
        private final AtomicBoolean globalStateRecoveryAttempted = new AtomicBoolean(false);

        private static final List<String> GLOBAL_STATE_FILE_NAMES = List.of("___global.state", "___global.lck");

        private DefaultCacheManager startDefaultCacheManager() {
            var defaultCacheManager = new DefaultCacheManager(cacheManagerConfig, false);
            try {
                defaultCacheManager.start();
                return defaultCacheManager;
            } catch (RuntimeException reStart) {
                log.warn("Cannot start caching manager", reStart);
                try {
                    defaultCacheManager.stop();
                } catch (RuntimeException reStop) {
                    log.debug("Cannot stop failing caching manager", reStop);
                }
                if (isGlobalStateCorruption(reStart) && globalStateRecoveryAttempted.compareAndSet(false, true)) {
                    log.warn(
                        "Persistent global state under {} looks corrupted, most likely left behind by a process " +
                            "that was killed before it could stop cleanly (e.g. an abrupt system IPL); removing " +
                            "it and retrying cache manager startup once", InfinispanConfig.getRootFolder()
                    );
                    resetCorruptedGlobalState(Paths.get(InfinispanConfig.getRootFolder()));
                    return startDefaultCacheManager();
                }
                throw reStart;
            }
        }

        /**
         * ISPN000516 means Infinispan deliberately refused to start rather than risk further corrupting a
         * partially-written global state file. There is no way to repair the file, so the only way forward
         * is to discard it and let Infinispan recreate it; per-cache persisted data lives in separate
         * subdirectories and is left untouched.
         */
        static boolean isGlobalStateCorruption(Throwable t) {
            int idx = ExceptionUtils.indexOfType(t, EmbeddedCacheManagerStartupException.class);
            if (idx == -1) {
                return false;
            }
            var startupException = ExceptionUtils.getThrowables(t)[idx];
            return startupException.getMessage() != null && startupException.getMessage().contains("ISPN000516");
        }

        /**
         * The global state lives as two flat files directly under the root ("___global.state" and
         * "___global.lck"), structurally separate from each cache's own subdirectory ("&lt;root&gt;/&lt;cacheName&gt;/..").
         * Moving only those two files aside discards the corrupted state - so Infinispan
         * recreates it fresh - without touching any cache's persisted data, and keeps a backup of the corrupted files.
         */
        static void resetCorruptedGlobalState(Path rootDir) {
            if (!Files.isDirectory(rootDir)) {
                return;
            }
            long timestamp = System.currentTimeMillis();
            for (String fileName : GLOBAL_STATE_FILE_NAMES) {
                var path = rootDir.resolve(fileName);
                if (!Files.exists(path)) {
                    continue;
                }
                try {
                    var backupPath = rootDir.resolve(fileName + "-corrupt-" + timestamp);
                    Files.move(path, backupPath);
                    log.info("Backed up corrupted Infinispan global state file {} to {}", path, backupPath);
                } catch (IOException e) {
                    log.error("Failed to back up corrupted global state file {}", path, e);
                    throw new IllegalStateException("Cannot start cache", e);
                }
            }
        }

        /**
         * This method is responsible for initializing of CacheManager. The method could be called by
         * multiple threads. The aim is to split work in this case and then return the fully initialized
         * cache manager. In case of failure the original instance of lazy manager or partially initialized
         * bean could be returned. The next invocation should initiate it.
         * @return partially or fully initialized cache manager
         */
        public DefaultCacheManager getDefaultCacheManager() {
            try {
                // register to the barrier to check that the thread will leave method after each cache is initiated
                threadCounter.register();

                // start cache manager (only one thread could do that)
                synchronized (LazyCacheManager.class) {
                    if (underInit == null) {
                        log.debug("attempt to create cache manager");
                        try {
                            underInit = startDefaultCacheManager();
                            log.debug("cache manager was created");
                        } catch (Exception e) {
                            log.warn("Cannot initialize DefaultCacheManager", e);
                            return null;
                        }
                    }
                }

                while (true) {
                    String cacheName;
                    ConfigurationBuilder cacheBuilder;
                    // obtain name and builder of one cache (to initiate it only in one thread)
                    synchronized (LazyCacheManager.class) {
                        var i = caches.entrySet().iterator();
                        if (i.hasNext()) {
                            var entry = i.next();
                            cacheName = entry.getKey();
                            cacheBuilder = entry.getValue();
                            i.remove();
                        } else {
                            // if there is no cache to be initiated return instance and avoid using this method in the next calls
                            cacheManager.set(() -> underInit);
                            return underInit;
                        }
                    }

                    try {
                        createCache(cacheName, cacheBuilder);
                    } catch (Exception e) {
                        // initialization of cache failed. Put it back to be initialized again next invocation
                        synchronized (LazyCacheManager.class) {
                            caches.put(cacheName, cacheBuilder);
                        }
                        cacheManager.set(this::getDefaultCacheManager);
                        return underInit;
                    }
                }
            } finally {
                /**
                 * all caches should be initialized here or some caches initialization failed. Anyway, wait for other
                 * threads (to avoid partial initialization) before leaving the method. Waiting is limited by timeout
                 * defined in {@link #INIT_TIMEOUT_MINS}
                 */
                threadCounter.arriveAndDeregister();
                try {
                    threadCounter.awaitAdvanceInterruptibly(0, INIT_TIMEOUT_MINS, TimeUnit.MINUTES);
                } catch (InterruptedException ie) {
                    log.error("Thread was interrupted", ie);
                    Thread.currentThread().interrupt();
                } catch (TimeoutException te) {
                    log.warn("Timeout while initializing of caches: {}", te.getMessage());
                }
            }
        }

        private boolean createCache(String cacheName, ConfigurationBuilder cacheBuilder) {
            var cacheConfig = cacheBuilder.build();
            log.debug("Initializing cache {} with config {}", cacheName, cacheConfig);
            try {
                underInit.administration()
                    .withFlags(CacheContainerAdmin.AdminFlag.VOLATILE)
                    .getOrCreateCache(cacheName, cacheConfig);
                return true;
            } catch (CacheConfigurationException cce) {
                log.warn("Error during initialization of cache {}", cacheName, cce);
                try {
                    underInit.defineConfiguration(cacheName, cacheConfig);
                } catch (Exception e) {
                    log.warn("Configuration for cache {} already exists", cacheName, e);
                }
            } catch (Exception e) {
                log.warn("Error during initialization of cache {}", cacheName, e);
            }
            return false;
        }

        public boolean isInitialized() {
            return underInit != null && caches.isEmpty();
        }

        public void onApplicationStart() {
            if (!isInitialized() && (threadCounter.getUnarrivedParties() == 0)) {
                /**
                 * spring context is ready and no thread initialized the cache manager yet. Do it to avoid situation
                 * when service is not fully ready till any bean need a specific cache.
                 */
                getDefaultCacheManager();
            }
        }

    }

}
