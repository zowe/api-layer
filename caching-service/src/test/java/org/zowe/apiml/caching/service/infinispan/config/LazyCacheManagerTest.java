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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

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
        void whenGlobalStateFilesExist_thenOnlyThoseAreBackedUp() throws IOException {
            Files.createFile(rootDir.resolve("___global.state"));
            Files.createFile(rootDir.resolve("___global.lck"));
            var cacheDataDir = Files.createDirectories(rootDir.resolve("zoweCache").resolve("data"));
            var cacheEntry = Files.createFile(cacheDataDir.resolve("some.dat"));

            LazyCacheManager.CacheInitializer.resetCorruptedGlobalState(rootDir);

            assertFalse(Files.exists(rootDir.resolve("___global.state")));
            assertFalse(Files.exists(rootDir.resolve("___global.lck")));

            try (var stream = Files.list(rootDir)) {
                long backupCount = stream
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.startsWith("___global.state-corrupt-") || name.startsWith("___global.lck-corrupt-"))
                    .count();
                assertEquals(2, backupCount, "Expected exactly 2 backup files to be created");
            }

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

        @Test
        void whenFilesMoveFails_thenThrowsIllegalStateException() throws IOException {
            Files.createFile(rootDir.resolve("___global.state"));

            try (var filesMock = mockStatic(Files.class, CALLS_REAL_METHODS)) {
                filesMock.when(() -> Files.move(any(Path.class), any(Path.class)))
                    .thenThrow(new IOException("simulated move failure"));

                IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    LazyCacheManager.CacheInitializer.resetCorruptedGlobalState(rootDir)
                );

                assertEquals("Cannot start cache", ex.getMessage());
                assertInstanceOf(IOException.class, ex.getCause());
            }
        }
    }
}
