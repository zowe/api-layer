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
import java.util.Properties;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.util.ResourceUtils.CLASSPATH_URL_PREFIX;


public class VersionServiceTest {
    private static final String NO_VERSION = "Build information is not available";
    private static final String ZOWE_MANIFEST_FIELD = "zoweManifest";

    private VersionService versionService;

    @Before
    public void setup() {
        BuildInfo buildInfo = mock(BuildInfo.class);
        BuildInfoDetails buildInfoDetails = new BuildInfoDetails(new Properties(), new Properties());
        when(buildInfo.getBuildInfoDetails()).thenReturn(buildInfoDetails);
        versionService = new VersionService(buildInfo);
        versionService.clearVersionInfo();
    }

    @Test
    public void shouldReturnApimlVersion() {
        BuildInfo buildInfo = mock(BuildInfo.class);

        Properties buildProperties = new Properties();
        buildProperties.setProperty("build.version", "0.0.0");
        buildProperties.setProperty("build.number", "000");

        Properties gitProperties = new Properties();
        gitProperties.setProperty("git.commit.id.abbrev", "1a3b5c7");

        BuildInfoDetails buildInfoDetails = new BuildInfoDetails(buildProperties, gitProperties);
        when(buildInfo.getBuildInfoDetails()).thenReturn(buildInfoDetails);

        VersionService versionService = new VersionService(buildInfo);

        VersionInfo version = versionService.getVersion();
        assertNotNull(version.getApimlVersion());
        assertEquals("0.0.0 build #000 (1a3b5c7)", version.getApimlVersion());
    }

    @Test
    public void shouldReturnBuildUnavailableInApimlVersionWhenNoVersion() {
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

        VersionInfo version = versionService.getVersion();
        assertNotNull(version.getZoweVersion());
        assertEquals(NO_VERSION, version.getZoweVersion());
    }

    @Test
    public void shouldReturnBuildUnavailableInZoweVersionWhenFileNotFound() {
        ReflectionTestUtils.setField(versionService, ZOWE_MANIFEST_FIELD, "zowe-manifesto.json");

        VersionInfo version = versionService.getVersion();
        assertNotNull(version.getZoweVersion());
        assertEquals(NO_VERSION, version.getZoweVersion());
    }
}
