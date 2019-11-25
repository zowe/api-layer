/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.util;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;

public class FileUtilsTest {

    @Test
    public void testLocateFileForNull() {
        File aFile = FileUtils.locateFile(null);
        assertNull(aFile);
    }

    /**
     * Locate file as resource or a System resource
     */
    @Test
    public void testLocateFileAsResource() {

        // Resource accessible from this class classloader
        String fileName = "/service-configuration.yml";

        File aFile = FileUtils.locateFile(fileName);
        assertNotNull(aFile);
        assertEquals(aFile.getName(), "service-configuration.yml");
        assertNotNull(aFile.canRead());

        // Resource accessible using System classloader
        fileName = "service-configuration.yml";
        aFile = FileUtils.locateFile(fileName);
        assertNotNull(aFile);
        assertEquals(aFile.getName(), "service-configuration.yml");
        assertNotNull(aFile.canRead());
    }
    @Test
    public void testLocateFileAsDirectory() {
        String fileName = "/";

        File aFile = FileUtils.locateDirectory(fileName);
        assertNotNull(aFile);
        assertNotNull(aFile.canRead());

        fileName = ".";
        aFile = FileUtils.locateDirectory(fileName);
        assertNotNull(aFile);
        assertTrue(aFile.getAbsolutePath().startsWith(System.getProperty("user.dir")));
    }

    @Test
    public void testLocateFileAbsolutePath() {
        String fileName = "c:\\Program Files\\desktop.ini";
        File aFile = FileUtils.locateFile(fileName);
        assertNotNull(aFile);
        assertNotNull(aFile.canRead());

        fileName = "c:/Program Files/desktop.ini";
        aFile = FileUtils.locateFile(fileName);
        assertNotNull(aFile);
        assertNotNull(aFile.canRead());
    }

    @Test
    public void loadFileFromRelativePath() {
        String fileName = "../";
        File aFile = FileUtils.locateDirectory(fileName);

        assertNotNull(aFile);
        assertNotNull(aFile.canRead());
    }
}
