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
import org.infinispan.Cache;
import org.infinispan.commons.configuration.ClassAllowList;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.global.GlobalConfiguration;
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
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class LazyCacheManager implements CacheContainer, EmbeddedCacheManager {

    private final Supplier<DefaultCacheManager> cacheContainerSupplier;

    private final AtomicReference<DefaultCacheManager> cacheContainer = new AtomicReference<>();

    private DefaultCacheManager getCacheContainer() {
        var container = cacheContainer.get();
        if (container == null) {
            synchronized (this) {
                container = cacheContainer.get();
                if (container == null) {
                    container = cacheContainerSupplier.get();
                    cacheContainer.set(container);
                }
            }
            if (container == null) {
                throw new IllegalStateException("Cache container is not initialized yet");
            }
        }
        return container;
    }

    @Override
    public Configuration defineConfiguration(String cacheName, Configuration configuration) {
        return getCacheContainer().defineConfiguration(cacheName, configuration);
    }

    @Override
    public Configuration defineConfiguration(String cacheName, String templateCacheName, Configuration configurationOverride) {
        return getCacheContainer().defineConfiguration(cacheName, templateCacheName, configurationOverride);
    }

    @Override
    public void undefineConfiguration(String configurationName) {
        getCacheContainer().undefineConfiguration(configurationName);
    }

    @Override
    public String getClusterName() {
        return getCacheContainer().getClusterName();
    }

    @Override
    public List<Address> getMembers() {
        return getCacheContainer().getMembers();
    }

    @Override
    public Address getAddress() {
        return getCacheContainer().getAddress();
    }

    @Override
    public Address getCoordinator() {
        return getCacheContainer().getCoordinator();
    }

    @Override
    public boolean isCoordinator() {
        return getCacheContainer().isCoordinator();
    }

    @Override
    public ComponentStatus getStatus() {
        return getCacheContainer().getStatus();
    }

    @Override
    public GlobalConfiguration getCacheManagerConfiguration() {
        return getCacheContainer().getCacheManagerConfiguration();
    }

    @Override
    public Configuration getCacheConfiguration(String name) {
        return getCacheContainer().getCacheConfiguration(name);
    }

    @Override
    public Configuration getDefaultCacheConfiguration() {
        return getCacheContainer().getDefaultCacheConfiguration();
    }

    @Override
    public Set<String> getAccessibleCacheNames() {
        return getCacheContainer().getAccessibleCacheNames();
    }

    @Override
    public boolean isRunning(String cacheName) {
        return getCacheContainer().isRunning(cacheName);
    }

    @Override
    public boolean isDefaultRunning() {
        return getCacheContainer().isDefaultRunning();
    }

    @Override
    public boolean cacheExists(String cacheName) {
        return getCacheContainer().cacheExists(cacheName);
    }

    @Override
    public boolean cacheConfigurationExists(String name) {
        return getCacheContainer().cacheConfigurationExists(name);
    }

    @Override
    public <K, V> Cache<K, V> getCache() {
        return getCacheContainer().getCache();
    }

    @Override
    public <K, V> Cache<K, V> getCache(String cacheName) {
        return getCacheContainer().getCache(cacheName);
    }

    @Override
    public <K, V> Cache<K, V> createCache(String name, Configuration configuration) {
        return getCacheContainer().createCache(name, configuration);
    }

    @Override
    public <K, V> Cache<K, V> getCache(String cacheName, boolean createIfAbsent) {
        return getCacheContainer().getCache(cacheName, createIfAbsent);
    }

    @Override
    public EmbeddedCacheManager startCaches(String... cacheNames) {
        return getCacheContainer().startCaches(cacheNames);
    }

    @Override
    public void addCacheDependency(String from, String to) {
        getCacheContainer().addCacheDependency(from, to);
    }

    @Override
    public CacheContainerStats getStats() {
        return getCacheContainer().getStats();
    }

    @Override
    public Health getHealth() {
        return getCacheContainer().getHealth();
    }

    @Override
    public CacheManagerInfo getCacheManagerInfo() {
        return getCacheContainer().getCacheManagerInfo();
    }

    @Override
    public ClassAllowList getClassWhiteList() {
        return getCacheContainer().getClassWhiteList();
    }

    @Override
    public ClassAllowList getClassAllowList() {
        return getCacheContainer().getClassAllowList();
    }

    @Override
    public Subject getSubject() {
        return getCacheContainer().getSubject();
    }

    @Override
    public EmbeddedCacheManager withSubject(Subject subject) {
        return getCacheContainer().withSubject(subject);
    }

    @Override
    public Set<String> getCacheNames() {
        return getCacheContainer().getCacheNames();
    }

    @Override
    public void start() {
        getCacheContainer().start();
    }

    @Override
    public void stop() {
        getCacheContainer().stop();
    }

    public <T extends CacheContainer> T getOriginal() {
        return (T) getCacheContainer();
    }

    @Override
    public void close() throws IOException {
        getCacheContainer().close();
    }

    @Override
    public CompletionStage<Void> addListenerAsync(Object listener) {
        return getCacheContainer().addListenerAsync(listener);
    }

    @Override
    public CompletionStage<Void> removeListenerAsync(Object listener) {
        return getCacheContainer().removeListenerAsync(listener);
    }

}
