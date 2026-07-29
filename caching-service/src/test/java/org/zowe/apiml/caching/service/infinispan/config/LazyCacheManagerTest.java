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

import org.infinispan.manager.EmbeddedCacheManagerStartupException;
import org.infinispan.persistence.spi.PersistenceException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

class LazyCacheManagerTest {

    @Nested
    class IsGlobalStateCorruption {

        @Test
        void whenMessageContainsIspn000516_thenReturnTrue() {
            var exception = new EmbeddedCacheManagerStartupException("ISPN000516: The state file for '___global' is invalid");

            assertTrue(LazyCacheManager.CacheInitializer.isGlobalStateCorruption(exception));
        }

        @Test
        void whenIspn000516ExceptionIsWrappedInAnotherException_thenReturnTrue() {
            var startupException = new EmbeddedCacheManagerStartupException("ISPN000516: The state file for '___global' is invalid");
            var wrapper = new RuntimeException("cache manager failed to start", startupException);

            assertTrue(LazyCacheManager.CacheInitializer.isGlobalStateCorruption(wrapper));
        }

        @Test
        void whenExceptionMessageIsUnrelated_thenReturnFalse() {
            var exception = new EmbeddedCacheManagerStartupException("some other startup failure");

            assertFalse(LazyCacheManager.CacheInitializer.isGlobalStateCorruption(exception));
        }

        @Test
        void whenNoEmbeddedCacheManagerStartupExceptionInChain_thenReturnFalse() {
            assertFalse(LazyCacheManager.CacheInitializer.isGlobalStateCorruption(new IllegalStateException("boom")));
        }

        @Test
        void whenStartupExceptionHasNoMessage_thenReturnFalse() {
            assertFalse(LazyCacheManager.CacheInitializer.isGlobalStateCorruption(new EmbeddedCacheManagerStartupException()));
        }
    }

    @Nested
    class ResetCorruptedGlobalState {

        @TempDir
        Path rootDir;

        @Test
        void whenGlobalStateFilesExist_thenOnlyThoseAreRemoved() throws IOException {
            Files.createFile(rootDir.resolve("___global.state"));
            Files.createFile(rootDir.resolve("___global.lck"));
            var cacheDataDir = Files.createDirectories(rootDir.resolve("zoweCache").resolve("data"));
            var cacheEntry = Files.createFile(cacheDataDir.resolve("some.dat"));

            LazyCacheManager.CacheInitializer.resetCorruptedGlobalState(rootDir);

            assertFalse(Files.exists(rootDir.resolve("___global.state")));
            assertFalse(Files.exists(rootDir.resolve("___global.lck")));
            assertTrue(Files.exists(cacheEntry));
        }

        @Test
        void whenRootDirDoesNotExist_thenDoesNotThrow() {
            var missingDir = rootDir.resolve("does-not-exist");

            assertDoesNotThrow(() -> LazyCacheManager.CacheInitializer.resetCorruptedGlobalState(missingDir));
        }

        @Test
        void whenNoGlobalStateFilesPresent_thenNothingIsRemoved() throws IOException {
            var unrelated = Files.createFile(rootDir.resolve("unrelated.txt"));

            LazyCacheManager.CacheInitializer.resetCorruptedGlobalState(rootDir);

            assertTrue(Files.exists(unrelated));
        }
    }

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
            assertEquals("12", properties.getProperty("source.version"));

            assertEquals("SOFT_INDEX_FILE_STORE", properties.getProperty("target.type"));
            assertEquals("invalidatedJwtTokens", properties.getProperty("target.cache_name"));
            assertEquals(targetDir.resolve("data").toString(), properties.getProperty("target.location"));
            assertEquals(targetDir.resolve("index").toString(), properties.getProperty("target.index_location"));
        }
    }
}
