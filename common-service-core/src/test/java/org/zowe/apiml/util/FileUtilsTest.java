/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class FileUtilsTest {
    private static final String RELATIVE_PATH_NAME = "Documents/apiml";
    private static final String CONFIG_PATH = System.getProperty("user.home").replace("\\", "/") + File.separator + RELATIVE_PATH_NAME;
    private static boolean folderCreated = false;

    private static String fileName = "service-configuration.yml";
    private static File configFile;

    @BeforeAll
    static void setUp() {
        File customDir = new File(CONFIG_PATH);
        if (!customDir.exists()) {
            customDir.mkdirs();
            folderCreated = true;
        }

        if ((configFile == null) || !configFile.exists()) {
            configFile = new File(CONFIG_PATH + File.separator + fileName);
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @AfterAll
    static void cleanUp() {
        if ((configFile != null) && configFile.exists()) {
            configFile.delete();
        }

        if (folderCreated) {
            File documentsFolder = new File(CONFIG_PATH);
            documentsFolder.delete();
        }
    }

    @Test
    void testLocateFileForNull() {
        File aFile = FileUtils.locateFile(null);
        assertNull(aFile);
    }

    /**
     * Locate file as resource or a System resource
     */
    @Test
    void testLocateFileAsResource() {

        // Resource accessible from this class classloader
        String fileName = "/service-configuration.yml";

        File aFile = FileUtils.locateFile(fileName);
        assertNotNull(aFile);
        assertEquals("service-configuration.yml", aFile.getName());
        assertTrue(aFile.canRead());

        // Resource accessible using System classloader
        fileName = "service-configuration.yml";
        aFile = FileUtils.locateFile(fileName);
        assertNotNull(aFile);
        assertEquals("service-configuration.yml", aFile.getName());
        assertTrue(aFile.canRead());
    }

    @Test
    void testLocateFileAsDirectory() {
        String fileName = "/";

        File aFile = FileUtils.locateDirectory(fileName);
        assertNotNull(aFile);
        assertTrue(aFile.canRead());

        fileName = ".";
        aFile = FileUtils.locateDirectory(fileName);
        assertNotNull(aFile);
        assertTrue(aFile.getAbsolutePath().startsWith(System.getProperty("user.dir")));

        aFile = FileUtils.locateDirectory(null);
        assertNull(aFile);
    }

    @Test
    void testLocateFileAbsolutePath() {
        File aFile = FileUtils.locateFile(CONFIG_PATH + File.separator + "DoesNotExist.file");
        assertNull(aFile);

        aFile = FileUtils.locateFile(CONFIG_PATH + File.separator + fileName);
        assertNotNull(aFile);
        assertTrue(aFile.canRead());

        File aDir = FileUtils.locateDirectory(CONFIG_PATH + File.separator + "NotExists.dir");
        assertNull(aDir);

        aDir = FileUtils.locateFile(CONFIG_PATH);
        assertNull(aDir);

        aDir = FileUtils.locateDirectory(CONFIG_PATH);
        assertNotNull(aDir);
        assertTrue(aDir.canRead());
    }

    @Test
    void testValidRelativePathExists() {
        File aFile = FileUtils.locateDirectory(RELATIVE_PATH_NAME);
        assertNotNull(aFile);
    }

    @Test
    void testValidRelativePathNotExists() {
        String fileName = "relative-path-not-exist/";
        File aFile = FileUtils.locateDirectory(fileName);
        assertNull(aFile);
    }

    @Test
    void testInvalidPathException() {
        String fileName = "invalid-path:\0/";
        assertThrows(InvalidPathException.class, () -> FileUtils.locateDirectory(fileName));
    }

    @Test
    void loadFileFromRelativePath() {
        String fileName = "../";
        File aFile = FileUtils.locateDirectory(fileName);

        assertNotNull(aFile);
        assertTrue(aFile.canRead());
    }

    @Test
    void testReadConfigurationFile_Existing() throws IOException {

        // 1) Existing file
        String internalFileName = "/service-configuration.yml";
        String result = FileUtils.readFile(internalFileName);
        assertNotNull(result);
        assertNotEquals(result.length(),  -1);
    }

    @Test
    void readNotExistingConfiguration() throws IOException {
        String fileData = FileUtils.readFile("no-existing-file");
        assertNull(fileData);
    }
}
