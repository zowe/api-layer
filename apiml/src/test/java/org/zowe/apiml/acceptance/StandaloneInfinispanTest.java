/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance;

import org.infinispan.configuration.cache.CacheMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Status;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.cache.Storage;
import org.zowe.apiml.caching.health.InfinispanHealthIndicator;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.infinispan.config.LazyCacheManager;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.zowe.apiml.cache.PATRevocationStore.INVALID_USERS_KEY;

@AcceptanceTest
@TestPropertySource(properties = {
    "caching.storage.mode=infinispan",
    "caching.storage.infinispan.standalone=true",
    "infinispan.embedded.enabled=true",
    "caching.storage.infinispan.initialHosts=invalid-host-list",
    "jgroups.bind.port=-1",
    "jgroups.keyExchange.port=-1",
    "apiml.enabled=false"
})
class StandaloneInfinispanTest {

    @Autowired
    private LazyCacheManager cacheManager;

    @Autowired
    private Storage storage;

    @Autowired
    private InfinispanHealthIndicator healthIndicator;

    @Test
    void startsModulithWithPersistentLocalCachesAndHealthyStorage() {
        assertTrue(cacheManager.isInitialized());
        assertFalse(cacheManager.getCacheManagerConfiguration().isClustered());
        assertNull(cacheManager.getAddress());
        assertNull(cacheManager.getMembers());
        for (String name : List.of("zoweCache", "zoweInvalidatedTokenCache", "zoweInvalidatedTokenItemCache", "invalidatedJwtTokens")) {
            var config = cacheManager.getCacheConfiguration(name);
            assertEquals(CacheMode.LOCAL, config.clustering().cacheMode());
            assertFalse(config.persistence().stores().isEmpty());
        }

        String timestamp = Long.toString(System.currentTimeMillis());
        storage.storeMapItem("standalone-test", INVALID_USERS_KEY, new KeyValue("user", timestamp));
        assertEquals(Map.of(INVALID_USERS_KEY, Map.of("user", timestamp)),
            storage.getMapItems("standalone-test", Map.of(INVALID_USERS_KEY, List.of("user"))));

        var health = healthIndicator.health();
        assertEquals(Status.UP, health.getStatus());
        assertEquals("standalone", ((Map<?, ?>) health.getDetails().get("infinispan")).get("mode"));
    }
}
