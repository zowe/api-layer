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

import io.micrometer.core.instrument.MeterRegistry;
import org.infinispan.configuration.cache.CacheMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.cache.Storage;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.config.ApplicationInfo;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;
import static org.zowe.apiml.cache.PATRevocationStore.INVALID_USERS_KEY;
import static org.zowe.apiml.caching.service.infinispan.config.InfinispanConfig.*;

class StandaloneInfinispanTest {

    @TempDir
    Path root;

    private MockedStatic<InfinispanConfig> rootFolder;

    @BeforeEach
    void configurePersistence() {
        rootFolder = mockStatic(InfinispanConfig.class);
        rootFolder.when(InfinispanConfig::getRootFolder).thenReturn(root.toString());
    }

    @AfterEach
    void restoreConfiguration() {
        rootFolder.close();
    }

    private InfinispanConfig configuration(boolean attls) {
        var config = new InfinispanConfig();
        ReflectionTestUtils.setField(config, "standalone", true);
        ReflectionTestUtils.setField(config, "isServerAttlsEnabled", attls);
        ReflectionTestUtils.setField(config, "numSegments", 16);
        ReflectionTestUtils.setField(config, "revocationStoreMaxCount", 2L);
        // No JGroups, keystore, or truststore properties are needed for local caches.
        return config;
    }

    private LazyCacheManager cacheManager(InfinispanConfig config, boolean modulith) {
        return config.cacheManager(new DefaultResourceLoader(), ApplicationInfo.builder().isModulith(modulith).build());
    }

    @Nested
    class Startup {

        @ParameterizedTest
        @CsvSource({"false,false", "false,true", "true,false", "true,true"})
        void startsPersistentCachesWithoutTransportOrClusteredLocks(boolean modulith, boolean attls) throws IOException {
            var config = configuration(attls);
            try (var manager = cacheManager(config, modulith)) {
                var storage = config.storage(manager, new StaticListableBeanFactory().getBeanProvider(MeterRegistry.class));
                storage.create("service", new KeyValue("key", "value"));

                assertTrue(manager.isInitialized());
                assertFalse(manager.getCacheManagerConfiguration().isClustered());
                assertNull(manager.getAddress());
                assertNull(manager.getMembers());
                assertFalse(manager.cacheExists("org.infinispan.LOCKS"));
                for (String name : List.of("default", CACHE_ZOWE, CACHE_ZOWE_INVALIDATED_TOKEN, CACHE_ZOWE_INVALIDATED_TOKEN_ITEM)) {
                    var cache = manager.getCacheConfiguration(name);
                    assertEquals(CacheMode.LOCAL, cache.clustering().cacheMode());
                    assertFalse(cache.persistence().stores().isEmpty());
                }
                assertEquals(modulith, manager.cacheExists("invalidatedJwtTokens"));
                if (modulith) {
                    assertEquals(CacheMode.LOCAL, manager.getCacheConfiguration("invalidatedJwtTokens").clustering().cacheMode());
                    assertTrue(manager.getCacheConfiguration("validatedJwtTokens").simpleCache());
                }
            }
        }

    }

    @Nested
    class Persistence {

        @Test
        void retainsExistingDataWhenSwitchingBetweenClusteredAndStandaloneModes() throws IOException {
            Properties originalProperties = (Properties) System.getProperties().clone();
            String timestamp = Long.toString(System.currentTimeMillis());
            int transportPort;
            int keyExchangePort;
            try (var transport = new ServerSocket(0); var keyExchange = new ServerSocket(0)) {
                transportPort = transport.getLocalPort();
                keyExchangePort = keyExchange.getLocalPort();
            }
            var clustered = new ApplicationContextRunner()
                .withUserConfiguration(InfinispanConfig.class)
                .withBean(ApplicationInfo.class, () -> ApplicationInfo.builder().isModulith(true).build())
                .withPropertyValues(
                    "caching.storage.mode=infinispan",
                    "caching.storage.infinispan.numSegments=16",
                    "caching.storage.infinispan.initialHosts=localhost[" + transportPort + "]",
                    "jgroups.bind.address=localhost",
                    "jgroups.bind.port=" + transportPort,
                    "jgroups.keyExchange.port=" + keyExchangePort,
                    "server.ssl.keyStoreType=PKCS12",
                    "server.ssl.keyStore=../keystore/service/service.keystore.p12",
                    "server.ssl.keyStorePassword=password",
                    "server.ssl.trustStoreType=PKCS12",
                    "server.ssl.trustStore=../keystore/service/service.truststore.p12",
                    "server.ssl.trustStorePassword=password"
                );
            try {
                clustered.run(context -> {
                    assertNull(context.getStartupFailure());
                    assertTrue(context.getBean(LazyCacheManager.class).getCacheManagerConfiguration().isClustered());
                    var storage = context.getBean(Storage.class);
                    storage.create("service", new KeyValue("key", "clustered"));
                    storage.storeMapItem("service", INVALID_USERS_KEY, new KeyValue("user", timestamp));
                });

                var config = configuration(false);
                try (var manager = cacheManager(config, true)) {
                    var storage = config.storage(manager, new StaticListableBeanFactory().getBeanProvider(MeterRegistry.class));
                    assertEquals("clustered", storage.read("service", "key").getValue());
                    assertEquals(timestamp, storage.getAllMapItems("service", INVALID_USERS_KEY).get("user"));
                    storage.update("service", new KeyValue("key", "standalone"));
                }

                clustered.run(context -> {
                    assertNull(context.getStartupFailure());
                    var storage = context.getBean(Storage.class);
                    assertEquals("standalone", storage.read("service", "key").getValue());
                    assertEquals(timestamp, storage.getAllMapItems("service", INVALID_USERS_KEY).get("user"));
                });
            } finally {
                System.setProperties(originalProperties);
            }
        }

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        void retainsCacheEntriesAndRevocationsAfterEvictionAndRestart(boolean modulith) throws IOException {
            var config = configuration(false);
            String timestamp = Long.toString(System.currentTimeMillis());
            var expectedRules = Map.of("user1", timestamp, "user2", timestamp, "user3", timestamp);
            try (var manager = cacheManager(config, modulith)) {
                var storage = config.storage(manager, new StaticListableBeanFactory().getBeanProvider(MeterRegistry.class));
                storage.create("service", new KeyValue("key", "value"));
                expectedRules.forEach((user, value) -> storage.storeMapItem("service", INVALID_USERS_KEY, new KeyValue(user, value)));
                assertEquals(expectedRules, storage.getMapItems("service", Map.of(INVALID_USERS_KEY, expectedRules.keySet())).get(INVALID_USERS_KEY));
                manager.getCache(CACHE_ZOWE_INVALIDATED_TOKEN).put("service" + INVALID_USERS_KEY, Map.of("legacyUser", timestamp));
                if (modulith) {
                    manager.getCache("invalidatedJwtTokens").put("jwt", true);
                }
            }

            try (var manager = cacheManager(config, modulith)) {
                var storage = config.storage(manager, new StaticListableBeanFactory().getBeanProvider(MeterRegistry.class));
                assertEquals("value", storage.read("service", "key").getValue());
                assertEquals(expectedRules, storage.getMapItems("service", Map.of(INVALID_USERS_KEY, expectedRules.keySet())).get(INVALID_USERS_KEY));
                assertEquals(timestamp, storage.getAllLegacyMaps("service").get(INVALID_USERS_KEY).get("legacyUser"));
                if (modulith) {
                    assertEquals(true, manager.getCache("invalidatedJwtTokens").get("jwt"));
                }
                storage.update("service", new KeyValue("key", "updated"));
                assertEquals("updated", storage.delete("service", "key").getValue());
            }
        }

    }
}
