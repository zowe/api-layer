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

import org.infinispan.persistence.spi.PersistenceException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

class LazyCacheManagerTest {

    @Nested
    class IsLegacyStoreFailure {

        @Test
        void whenCauseChainContainsPersistenceException_thenReturnTrue() {
            var cause = new PersistenceException("Found an invalid protobuf tag (1) having a field number smaller than 1");
            var wrapper = new CompletionException(cause);

            assertTrue(LazyCacheManager.CacheInitializer.isLegacyStoreFailure(wrapper));
        }

        @Test
        void whenExceptionItselfIsPersistenceException_thenReturnTrue() {
            assertTrue(LazyCacheManager.CacheInitializer.isLegacyStoreFailure(new PersistenceException("boom")));
        }

        @Test
        void whenCauseChainHasNoPersistenceException_thenReturnFalse() {
            var unrelated = new IllegalStateException("permission denied", new IOException("disk full"));

            assertFalse(LazyCacheManager.CacheInitializer.isLegacyStoreFailure(unrelated));
        }

        @Test
        void whenExceptionHasNoCause_thenReturnFalse() {
            assertFalse(LazyCacheManager.CacheInitializer.isLegacyStoreFailure(new IllegalStateException("no cause here")));
        }
    }

    @Nested
    class MigrationProperties {

        @Test
        void whenCalled_thenBuildsSourceAndTargetProperties() {
            var sourceDir = Path.of("caching-service", "localhost", "invalidatedJwtTokens");
            var targetDir = Path.of("caching-service", "localhost", "invalidatedJwtTokens-migrated");

            var properties = LazyCacheManager.CacheInitializer.migrationProperties("invalidatedJwtTokens", sourceDir, targetDir);

            assertEquals("SOFT_INDEX_FILE_STORE", properties.getProperty("source.type"));
            assertEquals("invalidatedJwtTokens", properties.getProperty("source.cache_name"));
            assertEquals(sourceDir.resolve("data").toString(), properties.getProperty("source.location"));
            assertEquals(sourceDir.resolve("index").toString(), properties.getProperty("source.index_location"));
            assertEquals("15", properties.getProperty("source.version"));

            assertEquals("SOFT_INDEX_FILE_STORE", properties.getProperty("target.type"));
            assertEquals("invalidatedJwtTokens", properties.getProperty("target.cache_name"));
            assertEquals(targetDir.resolve("data").toString(), properties.getProperty("target.location"));
            assertEquals(targetDir.resolve("index").toString(), properties.getProperty("target.index_location"));
        }
    }
}
