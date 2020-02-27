/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.product.version;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.util.ResourceUtils.CLASSPATH_URL_PREFIX;


public class VersionServiceTest {
    private static final String NO_VERSION = "Build information is not available";
    private static final String ZOWE_MANIFEST_FIELD = "zoweManifest";

    private BuildInfoDetails buildInfo;
    private VersionService versionService;

    @Before
    public void setup() {
        buildInfo = mock(BuildInfoDetails.class);
        versionService = new VersionService(buildInfo);
        versionService.clearVersionInfo();
    }

    @Test
    public void shouldReturnApimlVersion() {
        when(buildInfo.getVersion()).thenReturn("1.4.0-SNAPSHOT");
        when(buildInfo.getNumber()).thenReturn("n/a");
        when(buildInfo.getCommitId()).thenReturn("953f26e");

        VersionInfo version = versionService.getVersion();
        assertNotNull(version.getApimlVersion());
        assertEquals("1.4.0-SNAPSHOT build #n/a (953f26e)", version.getApimlVersion());
    }

    @Test
    public void shouldReturnBuildUnavailableInApimlVersionWhenNoVersion() {
        when(buildInfo.getVersion()).thenReturn("unknown");

        VersionInfo version = versionService.getVersion();
        assertNotNull(version.getApimlVersion());
        assertEquals(NO_VERSION, version.getApimlVersion());
    }

    @Test
    public void shouldReturnZoweVersion() throws FileNotFoundException {
        File file = ResourceUtils.getFile(CLASSPATH_URL_PREFIX + "zowe-manifest.json");
        if (!file.exists()) {
            fail();
        }
        ReflectionTestUtils.setField(versionService, ZOWE_MANIFEST_FIELD, file.getAbsolutePath());
        when(buildInfo.getVersion()).thenReturn("unknown");

        VersionInfo version = versionService.getVersion();
        assertNotNull(version.getZoweVersion());
        assertEquals("1.8.0 build #802", version.getZoweVersion());
    }

    @Test
    public void shouldReturnZoweVersionWithoutBuild() throws FileNotFoundException {
        File file = ResourceUtils.getFile(CLASSPATH_URL_PREFIX + "zowe-manifest-no-build-info.json");
        if (!file.exists()) {
            fail();
        }
        ReflectionTestUtils.setField(versionService, ZOWE_MANIFEST_FIELD, file.getAbsolutePath());
        when(buildInfo.getVersion()).thenReturn("unknown");

        VersionInfo version = versionService.getVersion();
        assertNotNull(version.getZoweVersion());
        assertEquals("1.8.0 build #n/a", version.getZoweVersion());
    }

    @Test
    public void shouldReturnBuildUnavailableInZoweVersionWhenInvalidJson() throws FileNotFoundException {
        File file = ResourceUtils.getFile(CLASSPATH_URL_PREFIX + "zowe-manifest-invalid.json");
        if (!file.exists()) {
            fail();
        }
        ReflectionTestUtils.setField(versionService, ZOWE_MANIFEST_FIELD, file.getAbsolutePath());
        when(buildInfo.getVersion()).thenReturn("unknown");

        VersionInfo version = versionService.getVersion();
        assertNotNull(version.getZoweVersion());
        assertEquals(NO_VERSION, version.getZoweVersion());
    }

    @Test
    public void shouldReturnBuildUnavailableInZoweVersionWhenFileNotFound() {
        ReflectionTestUtils.setField(versionService, ZOWE_MANIFEST_FIELD, "zowe-manifesto.json");
        when(buildInfo.getVersion()).thenReturn("unknown");

        VersionInfo version = versionService.getVersion();
        assertNotNull(version.getZoweVersion());
        assertEquals(NO_VERSION, version.getZoweVersion());
    }
}
