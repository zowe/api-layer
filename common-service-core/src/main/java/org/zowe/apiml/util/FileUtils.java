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

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class FileUtils {
    private FileUtils() {}

    /**
     * Searches for file with given name.
     * If a file object with that name exists, but is not a file, this method returns NULL.
     *
     * @param fileName - a file name to try to locate.
     * @return the located file or null
     */
    public static File locateFile(String fileName) {
        if (fileName == null) {
            return null;
        }

        File aFile = locateFileOrDirectory(fileName);
        if ((aFile != null) && aFile.isFile()) {
            return aFile;
        }

        return null;
    }

    /**
     * Searches for directory with given name.
     * If a file object with that name exists, but is not a directory, this method returns NULL.
     * @param directoryName the name of the directory to be located
     * @return the located directory or null
     */
    public static File locateDirectory(String directoryName) {
        if (directoryName == null) {
            return null;
        }

        File aFile = locateFileOrDirectory(directoryName);
        if ((aFile != null) && aFile.isDirectory()) {
            return aFile;
        }

        return null;
    }

    /**
     * Tries to locate or in other words to verify that a file with name 'fileName' exists.
     * First tries to find the file as a resource somewhere on the application or System classpath.
     * Then tries to locate it using 'fileName' as an absolute path
     * Then the algorithm tries to locate the file using the 'fileName' an relative path in USER_HOME or the current working directory.
     * Finally attempt is made to locate the file directly in any of the available file system roots.
     *
     * This method never throw exceptions.
     * Returns null If fileName is null or file is not found neither as Java (system) resource, nor as file on the file system.
     *
     * @param fileName file name to locate
     * @return the located file
     */
    private static File locateFileOrDirectory(String fileName) {
        // Try to find the file as a resource - application local or System resource
        URL fileUrl = getResourceUrl(fileName);
        if (fileUrl != null) {
            return new File(fileUrl.getFile());
        }

        File file = null;
            Path path = Paths.get(fileName);
        if (path != null) {
            if (path.isAbsolute()) {
                return path.toFile();
            }

            file = Paths.get(System.getProperty("user.dir")).resolve(path).toFile();
            if ((file != null) && file.canRead()) {
                return file;
            }

            file = Paths.get(System.getProperty("user.home")).resolve(path).toFile();
            if ((file != null) && file.canRead()) {
                return file;
            }
        }

        return file;
    }

    private static URL getResourceUrl(String fileName) {
        URL fileUrl = FileUtils.class.getResource(fileName);
        if (fileUrl == null) {
            log.debug(String.format("File resource [%s] can't be found by this class classloader. We'll try with SystemClassLoader...", fileName));

            fileUrl = ClassLoader.getSystemResource(fileName);
            if (fileUrl == null) {
                log.debug(String.format("File resource [%s] can't be found by SystemClassLoader.", fileName));
            }
        }
        return fileUrl;
    }

    /**
     * Reads data from a file as text.
     *
     * @param fileName - file name string to look for.
     * @return the file contents as String
     * @throws IOException - if file can't be read.
     */
    public static String readFile(String fileName) throws IOException {
        String fileData = null;

        File file = FileUtils.locateFile(fileName);
        if (file != null) {
            fileData = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
        }

        return fileData;
    }
}
