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
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class InfinispanConfigTest {

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

    }

    @Nested
    class GlobalConfiguration {

        private static final String INSTANCE = "ZWE_haInstance_id";
        private static final String WORKSPACE = "ZWE_zowe_workspaceDirectory";

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

    }

}
