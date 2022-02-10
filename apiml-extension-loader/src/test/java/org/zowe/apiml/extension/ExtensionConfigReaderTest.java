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

import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import com.google.common.io.Resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ExtensionConfigReaderTest {

    @Mock
    private ZoweRuntimeEnvironment environment;

    private ExtensionConfigReader configReader;

    @BeforeEach
    public void setUp() {
        this.configReader = new ExtensionConfigReader(environment);
        // setup example files
    }

    @Test
    public void testGetInstalledExtensions() {

        String[] basePackages = configReader.getBasePackages();


    }

    @Test
    public void testGetBasePackages() {
        when(environment.getPluginsDir()).thenReturn(Optional.of(Resources.getResource("manifest.yaml").getPath()));
        when(environment.getInstalledComponents()).thenReturn(Collections.singletonList("customextension"));
        when(environment.getEnabledComponents()).thenReturn(Collections.singletonList("customextension"));


    }

}
