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

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalStateConfiguration;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LazyCacheManagerRetryTest {

    private static final String MINIMAL_INFINISPAN_CONFIG =
        "<infinispan>" +
        "  <cache-container>" +
        "    <transport cluster=\"test-cluster\"/>" +
        "  </cache-container>" +
        "</infinispan>";

    private static ConfigurationBuilderHolder createMinimalHolder() {
        var holder = new ParserRegistry().parse(MINIMAL_INFINISPAN_CONFIG, MediaType.APPLICATION_XML);
        holder.getGlobalConfigurationBuilder().globalState().persistentLocation("/tmp/test-infinispan-state").enable();
        return holder;
    }

    private Object getCacheInitializer(LazyCacheManager lazyCacheManager) {
        return ReflectionTestUtils.getField(lazyCacheManager, "cacheInitializer");
    }

    private void setUnderInit(Object cacheInitializer, DefaultCacheManager dcm) {
        ReflectionTestUtils.setField(cacheInitializer, "underInit", dcm);
    }

    // ===================== 1. DefensiveDelegationGuards =====================

    @Nested
    class DefensiveDelegationGuards {

        private LazyCacheManager lazyCacheManager;
        private DefaultCacheManager mockDcm;

        @BeforeEach
        void setUp() {
            ConfigurationBuilderHolder holder = createMinimalHolder();
            Map<String, ConfigurationBuilder> caches = new HashMap<>();
            caches.put("testCache", new ConfigurationBuilder());
            lazyCacheManager = new LazyCacheManager(holder, caches, 3, 5000, 10000);

            // Inject a mock DCM via cacheManager producer to bypass initialization
            mockDcm = mock(DefaultCacheManager.class);
            when(mockDcm.getAccessibleCacheNames()).thenReturn(Collections.singleton("someCache"));
            when(mockDcm.getCacheNames()).thenReturn(Collections.singleton("someCache"));

            // Replace the cacheManager AtomicReference producer to return our mock
            @SuppressWarnings("unchecked")
            var cacheManagerRef = (java.util.concurrent.atomic.AtomicReference<org.codehaus.commons.compiler.util.Producer<DefaultCacheManager>>)
                ReflectionTestUtils.getField(lazyCacheManager, "cacheManager");
            cacheManagerRef.set(() -> mockDcm);

            // Ensure cache initializer is NOT in initialized state regardless of test ordering/interaction
            Object cacheInit = getCacheInitializer(lazyCacheManager);
            ReflectionTestUtils.setField(cacheInit, "underInit", null);
        }

        @Test
        void testGetAccessibleCacheNamesReturnsEmptyWhenNotInitialized() {
            // cacheInitializer has null underInit + non-empty caches -> isInitialized() = false
            assertFalse(lazyCacheManager.isInitialized());
            assertEquals(Collections.emptySet(), lazyCacheManager.getAccessibleCacheNames());
        }

        @Test
        void testGetCacheNamesReturnsEmptyWhenNotInitialized() {
            assertFalse(lazyCacheManager.isInitialized());
            assertEquals(Collections.emptySet(), lazyCacheManager.getCacheNames());
        }
    }

    // ===================== 2. RetryScenarios =====================

    @Nested
    class RetryScenarios {

        private Object cacheInitializer;
        private DefaultCacheManager mockDcm;

        @BeforeEach
        void setUp() {
            ConfigurationBuilderHolder holder = createMinimalHolder();
            Map<String, ConfigurationBuilder> caches = new HashMap<>();
            LazyCacheManager lazyCacheManager = new LazyCacheManager(holder, caches, 3, 100, 5000);
            cacheInitializer = getCacheInitializer(lazyCacheManager);

            mockDcm = mock(DefaultCacheManager.class);
            // setup mock for cleanPersistentState chain
            GlobalConfiguration globalConfig = mock(GlobalConfiguration.class);
            GlobalStateConfiguration globalState = mock(GlobalStateConfiguration.class);
            when(globalConfig.globalState()).thenReturn(globalState);
            when(globalState.persistentLocation()).thenReturn("/tmp/test-state");
            when(mockDcm.getCacheManagerConfiguration()).thenReturn(globalConfig);
        }

        @Test
        void testSuccessfulFirstAttempt() {
            // DCM starts successfully on first attempt
            doNothing().when(mockDcm).start();

            // invoke startCacheManagerInstance via reflection
            ReflectionTestUtils.invokeMethod(cacheInitializer, "startCacheManagerInstance", mockDcm);

            verify(mockDcm, times(1)).start();
            verify(mockDcm, never()).stop();
        }

        @Test
        void testRetryAfterFailure() {
            // DCM fails on first attempt, succeeds on second
            doThrow(new RuntimeException("JGroups timeout"))
                .doNothing()
                .when(mockDcm).start();
            when(mockDcm.getStatus()).thenReturn(ComponentStatus.TERMINATED);

            ReflectionTestUtils.invokeMethod(cacheInitializer, "startCacheManagerInstance", mockDcm);

            // start() called twice (1 fail + 1 success)
            verify(mockDcm, times(2)).start();
            // stopAndWaitForShutdown called after first failure
            verify(mockDcm, times(1)).stop();
        }

        @Test
        void testExhaustionThrowsException() {
            // DCM fails on all 3 attempts
            doThrow(new RuntimeException("JGroups timeout"))
                .when(mockDcm).start();
            when(mockDcm.getStatus()).thenReturn(ComponentStatus.TERMINATED);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                ReflectionTestUtils.invokeMethod(cacheInitializer, "startCacheManagerInstance", mockDcm)
            );

            assertTrue(ex.getMessage().contains("not initialized after 3 attempts"));
            verify(mockDcm, times(3)).start();
            verify(mockDcm, times(2)).stop(); // called after attempt 1 and 2

            // underInit must be null after exhaustion
            assertNull(ReflectionTestUtils.getField(cacheInitializer, "underInit"));
        }
    }

    // ===================== 3. DcmInstanceReuse =====================

    @Nested
    class DcmInstanceReuse {

        @Test
        void testSameDcmInstanceAcrossRetries() {
            ConfigurationBuilderHolder holder = createMinimalHolder();
            Map<String, ConfigurationBuilder> caches = new HashMap<>();
            LazyCacheManager lazyCacheManager = new LazyCacheManager(holder, caches, 3, 50, 5000);
            Object cacheInitializer = getCacheInitializer(lazyCacheManager);

            DefaultCacheManager mockDcm = mock(DefaultCacheManager.class);
            GlobalConfiguration globalConfig = mock(GlobalConfiguration.class);
            GlobalStateConfiguration globalState = mock(GlobalStateConfiguration.class);
            when(globalConfig.globalState()).thenReturn(globalState);
            when(globalState.persistentLocation()).thenReturn("/tmp/test-state");
            when(mockDcm.getCacheManagerConfiguration()).thenReturn(globalConfig);

            // Fail twice, succeed on third
            doThrow(new RuntimeException("fail 1"))
                .doThrow(new RuntimeException("fail 2"))
                .doNothing()
                .when(mockDcm).start();
            when(mockDcm.getStatus()).thenReturn(ComponentStatus.TERMINATED);

            ReflectionTestUtils.invokeMethod(cacheInitializer, "startCacheManagerInstance", mockDcm);

            // The same mock instance was used for all 3 start() calls
            verify(mockDcm, times(3)).start();
            // The same instance is stopped and restarted
            verify(mockDcm, times(2)).stop();
        }
    }

    // ===================== 4. BackoffRetryBehavior =====================

    @Nested
    class BackoffRetryBehavior {

        @Test
        void testLinearBackoffApplied() {
            ConfigurationBuilderHolder holder = createMinimalHolder();
            Map<String, ConfigurationBuilder> caches = new HashMap<>();
            long retryBackoffMs = 100;
            LazyCacheManager lazyCacheManager = new LazyCacheManager(holder, caches, 3, retryBackoffMs, 5000);
            Object cacheInitializer = getCacheInitializer(lazyCacheManager);

            DefaultCacheManager mockDcm = mock(DefaultCacheManager.class);
            GlobalConfiguration globalConfig = mock(GlobalConfiguration.class);
            GlobalStateConfiguration globalState = mock(GlobalStateConfiguration.class);
            when(globalConfig.globalState()).thenReturn(globalState);
            when(globalState.persistentLocation()).thenReturn("/tmp/test-state");
            when(mockDcm.getCacheManagerConfiguration()).thenReturn(globalConfig);

            // Fail twice, succeed on third
            doThrow(new RuntimeException("fail 1"))
                .doThrow(new RuntimeException("fail 2"))
                .doNothing()
                .when(mockDcm).start();
            when(mockDcm.getStatus()).thenReturn(ComponentStatus.TERMINATED);

            long startTime = System.currentTimeMillis();
            ReflectionTestUtils.invokeMethod(cacheInitializer, "startCacheManagerInstance", mockDcm);
            long elapsed = System.currentTimeMillis() - startTime;

            // Two backoff periods: 1*100ms + 2*100ms = 300ms minimum
            assertTrue(elapsed >= retryBackoffMs * 1 + retryBackoffMs * 2 - 20,
                "Expected elapsed time >= " + (retryBackoffMs * 3) + "ms but was " + elapsed + "ms");
        }
    }

    // ===================== 5. PersistentStateCleanup =====================

    @Nested
    class PersistentStateCleanup {

        private Object cacheInitializer;
        private DefaultCacheManager mockDcm;
        private Path tempDir;

        @BeforeEach
        void setUp() throws IOException {
            ConfigurationBuilderHolder holder = createMinimalHolder();
            Map<String, ConfigurationBuilder> caches = new HashMap<>();
            LazyCacheManager lazyCacheManager = new LazyCacheManager(holder, caches, 2, 50, 5000);
            cacheInitializer = getCacheInitializer(lazyCacheManager);

            mockDcm = mock(DefaultCacheManager.class);
            tempDir = Files.createTempDirectory("test-infinispan-state");
        }

        @Test
        void testPersistentStateCleanedBetweenRetries() throws IOException {
            // Create a test file in the temp directory
            Path testFile = tempDir.resolve("___global.state");
            Files.writeString(testFile, "test data");

            GlobalConfiguration globalConfig = mock(GlobalConfiguration.class);
            GlobalStateConfiguration globalState = mock(GlobalStateConfiguration.class);
            when(globalConfig.globalState()).thenReturn(globalState);
            when(globalState.persistentLocation()).thenReturn(tempDir.toString());
            when(mockDcm.getCacheManagerConfiguration()).thenReturn(globalConfig);

            // Directly call cleanPersistentState
            ReflectionTestUtils.invokeMethod(cacheInitializer, "cleanPersistentState", mockDcm);

            // The test file should be deleted
            assertFalse(Files.exists(testFile), "Persistent state file should be deleted after cleanup");
        }

        @Test
        void testPersistentStateCleanupHandlesException() throws IOException {
            GlobalConfiguration globalConfig = mock(GlobalConfiguration.class);
            GlobalStateConfiguration globalState = mock(GlobalStateConfiguration.class);
            when(globalConfig.globalState()).thenReturn(globalState);
            // Return a non-existent path — cleanup should not throw
            when(globalState.persistentLocation()).thenReturn("/nonexistent/path/12345");

            when(mockDcm.getCacheManagerConfiguration()).thenReturn(globalConfig);

            // Should not throw even though path doesn't exist
            assertDoesNotThrow(() ->
                ReflectionTestUtils.invokeMethod(cacheInitializer, "cleanPersistentState", mockDcm)
            );
        }
    }

    // ===================== 6. JGroupsShutdownVerification =====================

    @Nested
    class JGroupsShutdownVerification {

        private Object cacheInitializer;

        @BeforeEach
        void setUp() {
            ConfigurationBuilderHolder holder = createMinimalHolder();
            Map<String, ConfigurationBuilder> caches = new HashMap<>();
            // short timeout for testing
            LazyCacheManager lazyCacheManager = new LazyCacheManager(holder, caches, 3, 100, 500);
            cacheInitializer = getCacheInitializer(lazyCacheManager);
        }

        @Test
        void testStopAndWaitForShutdownPollsUntilTerminated() {
            DefaultCacheManager mockDcm = mock(DefaultCacheManager.class);
            // Return RUNNING first, then TERMINATED second (simulating async shutdown)
            when(mockDcm.getStatus())
                .thenReturn(ComponentStatus.RUNNING)
                .thenReturn(ComponentStatus.TERMINATED);

            ReflectionTestUtils.invokeMethod(cacheInitializer, "stopAndWaitForShutdown", mockDcm);

            verify(mockDcm, times(1)).stop();
            // getStatus should have been called at least twice: RUNNING → TERMINATED
            verify(mockDcm, atLeast(2)).getStatus();
        }

        @Test
        void testStopAndWaitForShutdownTimeout() {
            DefaultCacheManager mockDcm = mock(DefaultCacheManager.class);
            // Never reaches TERMINATED
            when(mockDcm.getStatus()).thenReturn(ComponentStatus.STOPPING);

            // Should complete without throwing even though TERMINATED is never reached
            assertDoesNotThrow(() ->
                ReflectionTestUtils.invokeMethod(cacheInitializer, "stopAndWaitForShutdown", mockDcm)
            );

            verify(mockDcm, times(1)).stop();
        }
    }
}
