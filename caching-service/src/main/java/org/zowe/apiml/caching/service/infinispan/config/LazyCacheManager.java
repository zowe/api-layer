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
import org.infinispan.Cache;
import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.commons.configuration.ClassAllowList;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.health.Health;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.CacheManagerInfo;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.stats.CacheContainerStats;

import javax.security.auth.Subject;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class LazyCacheManager extends DefaultCacheManager {

    private static final int RETRY = 2;

    private final AtomicReference<DefaultCacheManager> cacheManager = new AtomicReference<>();
    private final CacheInitializer cacheInitializer;

    public LazyCacheManager(
        ConfigurationBuilderHolder cacheManagerConfig,
        ConfigurationBuilder cacheConfig,
        Collection<String> cacheNames
    ) {
        super(cacheManagerConfig, false);
        cacheInitializer = new CacheInitializer(cacheManagerConfig, cacheConfig, new ArrayList<>(cacheNames));
    }

    private DefaultCacheManager getCacheManager() {
        var container = cacheManager.get();
        if (container == null) {
            synchronized (this) {
                container = cacheManager.get();
                if (container == null) {
                    container = cacheInitializer.getDefaultCacheManager();
                }
            }
            if (container == null) {
                throw new IllegalStateException("Cache container is not initialized yet");
            }
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
    public ClassAllowList getClassWhiteList() {
        return getCacheManager().getClassWhiteList();
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

    @RequiredArgsConstructor
    class CacheInitializer {

        private DefaultCacheManager underInit;

        private final ConfigurationBuilderHolder cacheManagerConfig;
        private final ConfigurationBuilder cacheConfig;
        private final Collection<String> cacheNames;

        public DefaultCacheManager getDefaultCacheManager() {
            if (underInit == null) {
                for (int i = 0; i < 1 + RETRY; i++) {
                    try {
                        underInit = new DefaultCacheManager(cacheManagerConfig, true);
                        break;
                    } catch (Exception e) {
                        log.warn("Cannot initialize DefaultCacheManager", e);
                    }
                }
            }

            if (underInit == null) {
                return null;
            }

            for (int i = 0; i < 1 + RETRY; i++) {
                if (createCaches()) {
                    break;
                }
            }

            if (cacheNames.isEmpty()) {
                cacheManager.set(underInit);
            }

            return underInit;
        }

        private boolean createCaches() {
            for (Iterator<String> i = cacheNames.iterator(); i.hasNext();) {
                String cacheName = i.next();
                if (createCache(cacheName)) {
                    i.remove();
                }
            }
            return cacheNames.isEmpty();
        }

        private boolean createCache(String cacheName) {
            try {
                underInit.administration()
                    .withFlags(CacheContainerAdmin.AdminFlag.VOLATILE)
                    .getOrCreateCache(cacheName, cacheConfig.build());
                return true;
            } catch (Exception e) {
                log.warn("Error during initialization of cache {}", cacheName, e);
                return false;
            }
        }

        public boolean isInitialized() {
            return underInit != null && cacheNames.isEmpty();
        }

    }

}
