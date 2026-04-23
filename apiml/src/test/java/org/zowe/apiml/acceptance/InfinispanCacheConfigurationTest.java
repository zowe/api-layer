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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.caching.service.infinispan.config.LazyCacheManager;

import java.time.Duration;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@AcceptanceTest
@TestPropertySource(properties = {
    "caching.storage.mode=infinispan",
    "caching.storage.infinispan.initialHosts=localhost[7800]",
    "infinispan.embedded.enabled=true",
    "jgroups.bind.port=7800",
    "jgroups.bind.address=localhost",
    "apiml.enabled=false"
})
class InfinispanCacheConfigurationTest {

    @Autowired
    private LazyCacheManager cacheManager;

    @ParameterizedTest
    @ValueSource(strings = {"zoweCache", "zoweInvalidatedTokenCache", "invalidatedJwtTokens"})
    void testDistributedCacheConfiguration(String cacheName) {
        var config = cacheManager.getCacheConfiguration(cacheName);

        assertEquals(CacheMode.REPL_SYNC, config.clustering().cacheMode());
    }

    @ParameterizedTest
    @MethodSource("cacheConfigurationsForValidation")
    void testZosmfSmallCacheConfiguration(String cacheName, long maxCount,  Duration expiration) {
        var config = cacheManager.getCacheConfiguration(cacheName);

        assertTrue(config.memory().isOffHeap());
        assertTrue(config.simpleCache());
        assertEquals(CacheMode.LOCAL, config.clustering().cacheMode());
        assertEquals(maxCount, config.memory().maxCount());
        assertEquals(expiration.toMillis(), config.expiration().lifespan());

        //When a new cache is defined, the test fails as reminder to cover the new configuration with a test
        assertEquals(11, cacheManager.getCacheNames().size());
    }

    private static Stream<Arguments> cacheConfigurationsForValidation() {
        return Stream.of(
            //cacheName, maxCount, lifespan
            arguments("validatedJwtTokens", 1000L, Duration.ofMinutes(1)),
            arguments("zosmfAuthenticationEndpoint", 10L, Duration.ofHours(1)),
            arguments("zosmfInfo", 10L, Duration.ofHours(1)),
            arguments("zosmfJwtEndpoint", 10L, Duration.ofHours(1)),
            arguments("trustedCertificates", 1000L, Duration.ofHours(1)),
            arguments("parseOIDCToken", 1000L, Duration.ofSeconds(20)),
            arguments("validationOIDCToken", 1000L, Duration.ofSeconds(20))
        );
    }

}
