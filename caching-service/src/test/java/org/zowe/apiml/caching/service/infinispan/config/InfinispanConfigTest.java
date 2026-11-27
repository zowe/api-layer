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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.caching.service.infinispan.exception.InfinispanConfigException;
import org.zowe.apiml.config.ApplicationInfo;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class InfinispanConfigTest {

    @SuppressWarnings("unchecked")
    Map<String, String> getEnvMap() {
        try {
            Class<?> envVarClass = System.getenv().getClass();
            Field mField = envVarClass.getDeclaredField("m");
            mField.setAccessible(true);
            return (Map<String, String>) mField.get(System.getenv());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail(e);
            return null;
        }
    }

    @Nested
    class Initialization {

        @Test
        void whenKeyringUrlIsInvalidAndMissingPassword_thenFixKeyringUrlAndSetPassword() {
            InfinispanConfig infinispanConfig = new InfinispanConfig();
            ReflectionTestUtils.setField(infinispanConfig, "keyStore", "safkeyringpce:///userId/ringId");
            infinispanConfig.updateKeyring();
            assertEquals("safkeyringpce://userId/ringId", ReflectionTestUtils.getField(infinispanConfig, "keyStore"));
            assertEquals("password", ReflectionTestUtils.getField(infinispanConfig, "keyStorePass"));
        }

        @Test
        void whenKeyringUrlIsInvalidAndSetPassword_thenFixKeyringUrl() {
            InfinispanConfig infinispanConfig = new InfinispanConfig();
            ReflectionTestUtils.setField(infinispanConfig, "keyStore", "safkeyring:///userId/ringId");
            ReflectionTestUtils.setField(infinispanConfig, "keyStorePass", "pswd");
            infinispanConfig.updateKeyring();
            assertEquals("safkeyring://userId/ringId", ReflectionTestUtils.getField(infinispanConfig, "keyStore"));
            assertEquals("pswd", ReflectionTestUtils.getField(infinispanConfig, "keyStorePass"));
        }

        @Test
        void whenKeystore_thenDontUpdate() {
            InfinispanConfig infinispanConfig = new InfinispanConfig();
            ReflectionTestUtils.setField(infinispanConfig, "keyStore", "/path");
            ReflectionTestUtils.setField(infinispanConfig, "keyStorePass", "pass");
            infinispanConfig.updateKeyring();
            assertEquals("/path", ReflectionTestUtils.getField(infinispanConfig, "keyStore"));
            assertEquals("pass", ReflectionTestUtils.getField(infinispanConfig, "keyStorePass"));
        }

        @Test
        void whenTruststoreKeyringUrlIsInvalidAndMissingPassword_thenFixTrustoreKeyringUrlAndSetPassword() {
            InfinispanConfig infinispanConfig = new InfinispanConfig();
            ReflectionTestUtils.setField(infinispanConfig, "trustStore", "safkeyringpce:///userId/ringId");
            infinispanConfig.updateKeyring();
            assertEquals("safkeyringpce://userId/ringId", ReflectionTestUtils.getField(infinispanConfig, "trustStore"));
            assertEquals("password", ReflectionTestUtils.getField(infinispanConfig, "trustStorePass"));
        }

        @Test
        void whenTruststoreKeyringUrlIsInvalidAndSetPassword_thenFixTruststoreKeyringUrl() {
            InfinispanConfig infinispanConfig = new InfinispanConfig();
            ReflectionTestUtils.setField(infinispanConfig, "trustStore", "safkeyring:///userId/ringId");
            ReflectionTestUtils.setField(infinispanConfig, "trustStorePass", "pswd");
            infinispanConfig.updateKeyring();
            assertEquals("safkeyring://userId/ringId", ReflectionTestUtils.getField(infinispanConfig, "trustStore"));
            assertEquals("pswd", ReflectionTestUtils.getField(infinispanConfig, "trustStorePass"));
        }

        @Test
        void whenTruststore_thenDontUpdate() {
            InfinispanConfig infinispanConfig = new InfinispanConfig();
            ReflectionTestUtils.setField(infinispanConfig, "trustStore", "/path");
            ReflectionTestUtils.setField(infinispanConfig, "trustStorePass", "pass");
            infinispanConfig.updateKeyring();
            assertEquals("/path", ReflectionTestUtils.getField(infinispanConfig, "trustStore"));
            assertEquals("pass", ReflectionTestUtils.getField(infinispanConfig, "trustStorePass"));
        }

    }

    @Nested
    class GlobalConfiguration {

        private static final String INSTANCE = "ZWE_haInstance_id";
        private static final String WORKSPACE = "ZWE_zowe_workspaceDirectory";

        @BeforeEach
        @AfterEach
        void cleanUp() {
            getEnvMap().remove(INSTANCE);
            getEnvMap().remove(WORKSPACE);
        }

        @Test
        void givenNoEnvironmentValues_whenEvaluatingRootFolder_thenUseLocalhost() {
            assertEquals("caching-service" + File.separator + "localhost", InfinispanConfig.getRootFolder());
        }

        @Test
        void givenOnlyInstanceIdValues_whenEvaluatingRootFolder_thenUseRelativePath() {
            getEnvMap().put(INSTANCE, "myInstance");
            assertEquals("caching-service" + File.separator + "myInstance", InfinispanConfig.getRootFolder());
        }

        @Test
        void givenAllEnvironmentValues_whenEvaluatingRootFolder_thenUseExactLocation() {
            getEnvMap().put(INSTANCE, "lpar1");
            getEnvMap().put(WORKSPACE, "/some/path");
            assertEquals(File.separator + "some" + File.separator + "path" + File.separator + "caching-service" + File.separator + "lpar1", InfinispanConfig.getRootFolder());
        }

        @Test
        void givenOnlyWorkspaceValues_whenEvaluatingRootFolder_thenUseExactLocationWithLocalhost() {
            getEnvMap().put(WORKSPACE, "/another/path");
            assertEquals(File.separator + "another" + File.separator + "path" + File.separator + "caching-service" + File.separator + "localhost", InfinispanConfig.getRootFolder());

        }

        @Test
        void givenNoAttlsSetup_whenEvaluatingInfinispanConfigFile_thenReturnSslConfig() {
            assertEquals("infinispan.xml", new InfinispanConfig().getInfinispanConfigFile());
        }

    }

    @Nested
    @ExtendWith(OutputCaptureExtension.class)
    class StandaloneConfiguration {

        private final InfinispanConfig config = new InfinispanConfig();

        @ParameterizedTest
        @ValueSource(strings = {"apiml", "caching_service"})
        void rejectsMultipleExplicitlyEnabledInstancesOnTheSameHost(String component) {
            var environment = Map.of(
                "ZWE_haInstances_node_1_hostname", "same-host",
                "ZWE_haInstances_node_2_hostname", "same-host",
                "ZWE_haInstances_node_1_components_" + component + "_enabled", "true",
                "ZWE_haInstances_node_2_components_" + component + "_enabled", "true"
            );

            var exception = assertThrows(InfinispanConfigException.class,
                () -> config.validateStandaloneConfiguration(environment, component));

            assertTrue(exception.getMessage().contains("node_1"));
            assertTrue(exception.getMessage().contains("node_2"));
            assertTrue(exception.getMessage().contains("standalone=false"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"apiml", "caching_service"})
        void rejectsMultipleInstancesInheritingEnabledDefault(String component) {
            var environment = Map.of(
                "ZWE_haInstances_node_1_hostname", "host1",
                "ZWE_haInstances_node_2_hostname", "host2",
                "ZWE_haInstances_node_1_sysname", "SYS1",
                "ZWE_haInstances_node_2_sysname", "SYS2",
                "ZWE_components_" + component + "_enabled", "true"
            );

            assertThrows(InfinispanConfigException.class,
                () -> config.validateStandaloneConfiguration(environment, component));
        }

        @ParameterizedTest
        @ValueSource(strings = {"apiml", "caching_service"})
        void permitsOneEnabledInstanceAndDisabledPeer(String component) {
            var environment = Map.of(
                "ZWE_haInstance_id", "NODE_1",
                "ZWE_haInstances_node_1_hostname", "host1",
                "ZWE_haInstances_node_2_hostname", "host2",
                "ZWE_haInstances_node_1_sysname", "SYS1",
                "ZWE_haInstances_node_2_sysname", "SYS2",
                "ZWE_components_" + component + "_enabled", "true",
                "ZWE_haInstances_node_2_components_" + component + "_enabled", "false"
            );

            assertDoesNotThrow(() -> config.validateStandaloneConfiguration(environment, component));
        }

        @Test
        void disabledPeerDoesNotHideOtherEnabledInstances() {
            var environment = Map.of(
                "ZWE_haInstance_id", "node_1",
                "ZWE_haInstances_node_1_hostname", "host1",
                "ZWE_haInstances_node_2_hostname", "host2",
                "ZWE_haInstances_node_3_hostname", "host3",
                "ZWE_haInstances_node_1_sysname", "SYS1",
                "ZWE_haInstances_node_2_sysname", "SYS2",
                "ZWE_haInstances_node_3_sysname", "SYS3",
                "ZWE_components_apiml_enabled", "true",
                "ZWE_haInstances_node_3_components_apiml_enabled", "false"
            );

            assertThrows(InfinispanConfigException.class,
                () -> config.validateStandaloneConfiguration(environment, "apiml"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"apiml", "caching_service"})
        void doesNotApplyCurrentInstanceOverrideToOtherInstances(String component, CapturedOutput output) {
            var environment = Map.of(
                "ZWE_haInstance_id", "node_1",
                "ZWE_haInstances_node_1_hostname", "host1",
                "ZWE_haInstances_node_2_hostname", "host2",
                "ZWE_haInstances_node_1_sysname", "SYS1",
                "ZWE_haInstances_node_2_sysname", "SYS2",
                "ZWE_components_" + component + "_enabled", "true",
                "ZWE_haInstances_node_1_components_" + component + "_enabled", "true"
            );

            assertDoesNotThrow(() -> config.validateStandaloneConfiguration(environment, component));
            assertTrue(output.getOut().contains("cannot determine whether other"));
        }

        @Test
        void warnsWhenMultipleInstancesHaveNoEnabledFlags(CapturedOutput output) {
            var environment = Map.of(
                "ZWE_haInstances_node_1_sysname", "SYS1",
                "ZWE_haInstances_node_2_sysname", "SYS2"
            );

            assertDoesNotThrow(() -> config.validateStandaloneConfiguration(environment, "apiml"));
            assertTrue(output.getOut().contains("token revocations are not replicated"));
        }

        @Test
        void permitsFreshLocalInstallWithoutTopology() {
            assertDoesNotThrow(() -> config.validateStandaloneConfiguration(Map.of(), "apiml"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"apiml", "caching_service"})
        void permitsOneDeclaredInstance(String component) {
            var environment = Map.of(
                "ZWE_haInstances_node_1_hostname", "host1",
                "ZWE_haInstances_node_1_sysname", "SYS1",
                "ZWE_components_" + component + "_enabled", "true"
            );

            assertDoesNotThrow(() -> config.validateStandaloneConfiguration(environment, component));
        }

        @Test
        void ignoresInstancesRunningUnrelatedComponents() {
            var environment = Map.of(
                "ZWE_components_apiml_enabled", "false",
                "ZWE_haInstances_node_1_sysname", "SYS1",
                "ZWE_haInstances_node_2_sysname", "SYS2",
                "ZWE_haInstances_node_3_sysname", "SYS3",
                "ZWE_haInstances_node_1_components_apiml_enabled", "true",
                "ZWE_haInstances_node_2_components_discovery_enabled", "true",
                "ZWE_haInstances_node_3_components_discovery_enabled", "true"
            );

            assertDoesNotThrow(() -> config.validateStandaloneConfiguration(environment, "apiml"));
        }

        @Test
        void componentHostnamesAreNotAdditionalInstances(CapturedOutput output) {
            var environment = Map.of(
                "ZWE_components_apiml_enabled", "true",
                "ZWE_haInstances_node_1_sysname", "SYS1",
                "ZWE_haInstances_node_1_hostname", "host1",
                "ZWE_haInstances_node_1_components_discovery_apiml_service_hostname", "discovery-host",
                "ZWE_haInstances_node_1_components_gateway_apiml_service_hostname", "gateway-host"
            );

            assertDoesNotThrow(() -> config.validateStandaloneConfiguration(environment, "apiml"));
            assertFalse(output.getOut().contains("Standalone Infinispan"));
        }

        @Test
        void rejectsContradictoryConfigurationBeforeLoadingCaches() {
            var environment = getEnvMap();
            var overrides = Map.of(
                "ZWE_haInstances_guard_test_1_components_apiml_enabled", "true",
                "ZWE_haInstances_guard_test_2_components_apiml_enabled", "true"
            );
            var previous = new HashMap<String, String>();
            overrides.keySet().forEach(key -> previous.put(key, environment.get(key)));
            try {
                environment.putAll(overrides);
                ReflectionTestUtils.setField(config, "standalone", true);
                var resourceLoader = mock(ResourceLoader.class);

                assertThrows(InfinispanConfigException.class,
                    () -> config.cacheManager(resourceLoader, ApplicationInfo.builder().isModulith(true).build()));
                verifyNoInteractions(resourceLoader);
            } finally {
                previous.forEach((key, value) -> {
                    if (value == null) {
                        environment.remove(key);
                    } else {
                        environment.put(key, value);
                    }
                });
            }
        }

    }

    @Nested
    class ClusterConfiguration {

        private static final String PORT = "7600";
        private static final String HOSTNAME = "hostname";

        @BeforeEach
        @AfterEach
        void cleanUp() {
            getEnvMap().keySet().removeIf(key -> key.startsWith("ZWE_haInstances_"));
        }

        @Test
        void givenInitialHostsIsAlreadySet_whenGetInitialHosts_thenReturnPredefinedValue() {
            InfinispanConfig config = new InfinispanConfig();
            ReflectionTestUtils.setField(config, "initialHosts", "customHost[1234]");

            String result = ReflectionTestUtils.invokeMethod(config, "getInitialHosts");

            assertEquals("customHost[1234]", result);
        }

        @Test
        void givenNoInitialHostsAndHa_whenGetInitialHosts_thenParseAndReturnCombinedList() {
            InfinispanConfig config = new InfinispanConfig();
            ReflectionTestUtils.setField(config, "port", PORT);

            getEnvMap().put("ZWE_haInstances_LPAR3_hostname", "lpr3.com");
            getEnvMap().put("ZWE_haInstances_LPAR_4_hostname", "lpr4.com");

            String result = ReflectionTestUtils.invokeMethod(config, "getInitialHosts");

            String expected1 = "lpr3.com[7600],lpr4.com[7600]";
            String expected2 = "lpr4.com[7600],lpr3.com[7600]";

            assertNotNull(result);
            assertTrue(result.equals(expected1) || result.equals(expected2),
                "Result was: " + result);
        }

        @Test
        void givenNoInitialHostsAndNoHa_whenGetInitialHosts_thenFallbackToLocalhost() {
            InfinispanConfig config = new InfinispanConfig();
            ReflectionTestUtils.setField(config, "port", PORT);
            ReflectionTestUtils.setField(config, "hostname", HOSTNAME);

            String result = ReflectionTestUtils.invokeMethod(config, "getInitialHosts");

            assertEquals("hostname[7600]", result);
        }

        @Test
        void whenCheckingDiscoveryRunsValue_thenEnsureFieldIsCorrectlySet() {
            InfinispanConfig config = new InfinispanConfig();
            ReflectionTestUtils.setField(config, "discoveryRuns", 20);

            assertEquals(20, ReflectionTestUtils.getField(config, "discoveryRuns"));
        }
    }

}
