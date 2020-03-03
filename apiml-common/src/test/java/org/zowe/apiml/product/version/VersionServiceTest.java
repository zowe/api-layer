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
    private static final String ZOWE_MANIFEST_FIELD = "zoweManifest";

    private VersionService versionService;

    @Before
    public void setup() {
        BuildInfo buildInfo = mock(BuildInfo.class);
        BuildInfoDetails buildInfoDetails = new BuildInfoDetails(new Properties(), new Properties());
        when(buildInfo.getBuildInfoDetails()).thenReturn(buildInfoDetails);
        versionService = new VersionService(buildInfo);
    }

    @Test
    public void shouldReturnSpecificApimlVersion() {
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
        assertNotNull(version.getApiml());
        assertEquals("0.0.0", version.getApiml().getVersion());
        assertEquals("000", version.getApiml().getBuildNumber());
        assertEquals("1a3b5c7", version.getApiml().getCommitHash());
    }

    @Test
    public void shouldReturnUnknownInApimlVersionWhenNoVersion() {
        VersionInfo version = versionService.getVersion();
        assertNotNull(version.getApiml());
        assertEquals("Unknown", version.getApiml().getVersion());
        assertEquals("null", version.getApiml().getBuildNumber());
        assertEquals("Unknown", version.getApiml().getCommitHash());
    }

    @Test
    public void shouldReturnSpecificZoweVersion() throws FileNotFoundException {
        File file = ResourceUtils.getFile(CLASSPATH_URL_PREFIX + "zowe-manifest.json");
        ReflectionTestUtils.setField(versionService, ZOWE_MANIFEST_FIELD, file.getAbsolutePath());

        VersionInfo version = versionService.getVersion();
        assertNotNull(version.getZowe());
        assertEquals("1.8.0", version.getZowe().getVersion());
        assertEquals("802", version.getZowe().getBuildNumber());
        assertEquals("397a4365056685d639810a077a58b736db9f018b", version.getZowe().getCommitHash());
    }

    @Test
    public void shouldReturnUnknownZoweVersionWhenNoBuildAndVersion() throws FileNotFoundException {
        File file = ResourceUtils.getFile(CLASSPATH_URL_PREFIX + "zowe-manifest-no-build-info.json");
        ReflectionTestUtils.setField(versionService, ZOWE_MANIFEST_FIELD, file.getAbsolutePath());

        VersionInfo version = versionService.getVersion();
        assertNotNull(version.getZowe());
        assertEquals("Unknown", version.getZowe().getVersion());
        assertEquals("null", version.getZowe().getBuildNumber());
        assertEquals("Unknown", version.getZowe().getCommitHash());
    }

    @Test
    public void shouldReturnNullInZoweVersionWhenInvalidJson() throws FileNotFoundException {
        File file = ResourceUtils.getFile(CLASSPATH_URL_PREFIX + "zowe-manifest-invalid.json");
        ReflectionTestUtils.setField(versionService, ZOWE_MANIFEST_FIELD, file.getAbsolutePath());

        VersionInfo version = versionService.getVersion();
        assertNull(version.getZowe());
    }

    @Test
    public void shouldReturnNullInZoweVersionWhenFileNotFound() {
        ReflectionTestUtils.setField(versionService, ZOWE_MANIFEST_FIELD, "zowe-manifesto.json");

        VersionInfo version = versionService.getVersion();
        assertNull(version.getZowe());
    }
}
