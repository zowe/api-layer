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

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.when;

import java.util.Collections;

import com.google.common.io.Resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExtensionConfigReaderTest {

    @Mock
    private ZoweRuntimeEnvironment environment;

    private ExtensionConfigReader configReader;

    @BeforeEach
    public void setUp() {
        this.configReader = new ExtensionConfigReader(environment);
    }

    private String getTestResourcesPath() {
        String path = Resources.getResource("apimlextension").getPath();
        return path.substring(0, path.lastIndexOf('/'));
    }

    @Nested
    class WhenGettingExtensionsConfiguration {

        @Nested
        class GivenNoManifestIsAvailable {
            @Test
            void itReturnsNoPackagesToScan() {
                when(environment.getExtensionDirectory()).thenReturn(".");
                when(environment.getInstalledComponents()).thenReturn(singletonList("apimlextension"));
                when(environment.getEnabledComponents()).thenReturn(singletonList("apimlextension"));

                assertArrayEquals(new String[]{}, configReader.getBasePackages());
            }
        }

        @Nested
        class GivenAnExtensionIsDefined {
            @Test
            void itReturnsPackageNameToScan() {
                when(environment.getExtensionDirectory()).thenReturn(getTestResourcesPath());
                when(environment.getInstalledComponents()).thenReturn(singletonList("apimlextension"));
                when(environment.getEnabledComponents()).thenReturn(singletonList("apimlextension"));

                assertArrayEquals(new String[]{ "org.zowe" }, configReader.getBasePackages());
            }
        }

        @Nested
        class GivenNoComponentsAreInstalled {
            @Test
            void itReturnsNoPackagesToScan() {
                when(environment.getInstalledComponents()).thenReturn(Collections.emptyList());
                when(environment.getEnabledComponents()).thenReturn(Collections.emptyList());

                assertArrayEquals(new String[]{}, configReader.getBasePackages());
            }
        }
    }
}
