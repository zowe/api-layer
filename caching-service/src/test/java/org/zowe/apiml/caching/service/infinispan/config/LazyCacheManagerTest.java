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

import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManagerStartupException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

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

    @Nested
    class StartDefaultCacheManager {

        LazyCacheManager.CacheInitializer cacheInitializer;

        @BeforeEach
        void setUp() {
            var lazyCacheManager = new LazyCacheManager(new ConfigurationBuilderHolder(), new HashMap<>());
            cacheInitializer = (LazyCacheManager.CacheInitializer) ReflectionTestUtils.getField(lazyCacheManager, "cacheInitializer");
        }

        private DefaultCacheManager invoke() {
            return ReflectionTestUtils.invokeMethod(cacheInitializer, "startDefaultCacheManager");
        }

        private AtomicBoolean globalStateRecoveryAttempted() {
            return (AtomicBoolean) ReflectionTestUtils.getField(cacheInitializer, "globalStateRecoveryAttempted");
        }

        @Test
        void whenStartSucceeds_thenReturnsStartedManager() {
            try (var mocked = mockConstruction(DefaultCacheManager.class)) {
                var result = invoke();

                assertSame(mocked.constructed().get(0), result);
                verify(result).start();
            }
        }

        @Test
        void whenStartFailsForUnrelatedReason_thenStopsAndRethrowsWithoutRecovery() {
            var startFailure = new IllegalStateException("boom");

            try (var mocked = mockConstruction(DefaultCacheManager.class, (mock, context) ->
                doThrow(startFailure).when(mock).start()
            )) {
                var thrown = assertThrows(IllegalStateException.class, this::invoke);

                assertSame(startFailure, thrown);
                verify(mocked.constructed().get(0)).stop();
                assertEquals(1, mocked.constructed().size());
            }
        }

        @Test
        void whenStartFailsAndStopAlsoFails_thenOriginalFailureIsStillThrown() {
            var startFailure = new IllegalStateException("start boom");

            try (var ignored = mockConstruction(DefaultCacheManager.class, (mock, context) -> {
                doThrow(startFailure).when(mock).start();
                doThrow(new RuntimeException("stop boom")).when(mock).stop();
            })) {
                var thrown = assertThrows(IllegalStateException.class, this::invoke);

                assertSame(startFailure, thrown);
            }
        }

        @Test
        void whenGlobalStateCorruptionDetected_thenRecoversAndRetriesStartupOnce(@TempDir Path root) {
            var corruptionFailure = new EmbeddedCacheManagerStartupException("ISPN000516: The state file for '___global' is invalid");

            try (var mocked = mockConstruction(DefaultCacheManager.class, (mock, context) -> {
                if (context.getCount() == 1) {
                    doThrow(corruptionFailure).when(mock).start();
                }
            }); var infinispanConfigMock = mockStatic(InfinispanConfig.class)) {
                infinispanConfigMock.when(InfinispanConfig::getRootFolder).thenReturn(root.toString());

                var result = invoke();

                assertEquals(2, mocked.constructed().size());
                assertSame(mocked.constructed().get(1), result);
                verify(mocked.constructed().get(0)).stop();
                verify(mocked.constructed().get(1)).start();
                assertTrue(globalStateRecoveryAttempted().get());
            }
        }

        @Test
        void whenGlobalStateCorruptionDetectedButRecoveryAlreadyAttempted_thenRethrowsWithoutRetry() {
            var corruptionFailure = new EmbeddedCacheManagerStartupException("ISPN000516: The state file for '___global' is invalid");
            globalStateRecoveryAttempted().set(true);

            try (var mocked = mockConstruction(DefaultCacheManager.class, (mock, context) ->
                doThrow(corruptionFailure).when(mock).start()
            )) {
                var thrown = assertThrows(EmbeddedCacheManagerStartupException.class, this::invoke);

                assertSame(corruptionFailure, thrown);
                assertEquals(1, mocked.constructed().size());
            }
        }
    }
}
