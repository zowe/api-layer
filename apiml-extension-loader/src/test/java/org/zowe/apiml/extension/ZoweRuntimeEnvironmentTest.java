/*
* This program and the accompanying materials are made available under the terms of the
* Eclipse Public License v2.0 which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Copyright Contributors to the Zowe Project.
*/
package org.zowe.apiml.extension;

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ZoweRuntimeEnvironmentTest {

    private final ZoweRuntimeEnvironment env = ZoweRuntimeEnvironment.defaultEnv();

    @Nested
    class WhenReadingEnvironmentInformation {

        @Nested
        class GivenEnvironmentIsTheDefault {

            @Test
            void pluginDirIsNotSetLocally() {
                assertSame(Optional.empty(), env.getPluginsDir());
            }

            @Test
            void installedComponentsAreNotSet() {
                assertEquals(0, env.getInstalledComponents().size());
            }

            @Test
            void enabledComponentsAreNotSet() {
                assertEquals(0, env.getEnabledComponents().size());
            }

            @Test
            void extensionDirectoryIsNotSet() {
                assertNull(env.getExtensionDirectory());
            }
        }
    }
}
