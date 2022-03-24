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

import java.nio.charset.Charset;
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

    private static final String EXTESION_IBM1047 = "apimlextension-ibm1047";
    private static final String EXTENSION_UTF8 = "apimlextension-utf8";

    @Mock
    private ZoweRuntimeEnvironment environment;

    private ExtensionConfigReader configReader;

    @BeforeEach
    public void setUp() {
        this.configReader = new ExtensionConfigReader(environment);
    }

    private String getTestResourcesPath(Charset charset) {
        String path;
        if (charset.equals(Charset.forName("IBM1047"))) {
            path = Resources.getResource(EXTESION_IBM1047).getPath();
        } else {
            path = Resources.getResource(EXTENSION_UTF8).getPath();
        }
        return path.substring(0, path.lastIndexOf('/'));
    }

    @Nested
    class WhenGettingExtensionsConfiguration {

        @Nested
        class GivenNoManifestIsAvailable {
            @Test
            void itReturnsNoPackagesToScan() {
                when(environment.getWorkspaceDirectory()).thenReturn(".");
                when(environment.getInstalledComponents()).thenReturn(singletonList(EXTENSION_UTF8));
                when(environment.getEnabledComponents()).thenReturn(singletonList(EXTENSION_UTF8));

                assertArrayEquals(new String[]{}, configReader.getBasePackages());
            }
        }

        @Nested
        class GivenEncodingIsEbcdic {
            @Test
            void itReturnsPackageNameToScan() {
                when(environment.getWorkspaceDirectory()).thenReturn(getTestResourcesPath(Charset.forName("IBM1047")));
                when(environment.getInstalledComponents()).thenReturn(singletonList(EXTESION_IBM1047));
                when(environment.getEnabledComponents()).thenReturn(singletonList(EXTESION_IBM1047));

                assertArrayEquals(new String[]{ "org.zowe" }, configReader.getBasePackages());
            }
        }

        @Nested
        class GivenAnExtensionIsDefined {
            @Test
            void itReturnsPackageNameToScan() {
                when(environment.getWorkspaceDirectory()).thenReturn(getTestResourcesPath(Charset.forName("UTF-8")));
                when(environment.getInstalledComponents()).thenReturn(singletonList(EXTENSION_UTF8));
                when(environment.getEnabledComponents()).thenReturn(singletonList(EXTENSION_UTF8));

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
