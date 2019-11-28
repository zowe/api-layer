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

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class FileUtils {
    /**
     * Searches for file with given name.
     * If a file object with that name exists, but is not a file, this method returns NULL.
     *
     * @param fileName - a file name to try to locate.
     * @return the located file or null
     */
    public static File locateFile(String fileName) {
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
        File aFile = locateFileOrDirectory(directoryName);
        if ((aFile != null) && aFile.isDirectory()) {
            return aFile;
        }

        return null;
    }

    /**
     * Tries to locate or in other words to verify that a file with name 'fileName' exists.
     * First tries to find the file as a resource somewhere on the application or System classpath.
     * Then tries to locate it using 'fileName' as as relative path
     * The final attempt is to locate the file using the 'fileName' an absolute path.  resolved from .
     *
     * This method never throw exceptions.
     * Returns null If fileName is null or file is not found neither as Java (system) resource, nor as file on the file system.
     *
     * @param fileName file name to locate
     * @return the located file
     */
    public static File locateFileOrDirectory(String fileName) {
        if (fileName == null) {
            return null;
        }
        // Try to find the file as a resource - application local or System resource
        URL fileUrl = null;
        try {
            fileUrl = ObjectUtil.getThisClass().getResource(fileName);
            if (fileUrl == null) {
                log.debug(String.format("File resource [%s] can't be found by this class classloader. We'll try with SystemClassLoader...", fileName));

                fileUrl = ClassLoader.getSystemResource(fileName);
                if (fileUrl == null) {
                    log.debug(String.format("File resource [%s] can't be found by SystemClassLoader.", fileName));
                }
            }
        } catch (Throwable t) {
            // Silently swallow the exceptions and try to find the file on the File System
            log.debug(String.format("File %s can't be found as Java resource. Exception was caught with the following message: [%s]", fileName, t.getMessage()));
        }

        File file = null;
        try {
            if (fileUrl != null) {
                file = new File(fileUrl.getFile());
            } else {
                Path path = null;
                try {
                    path = Paths.get(fileName);
                } catch (InvalidPathException ipe) {
                    log.error(String.format("Filename %s has invalid path.", fileName), ipe);
                }

                if (path != null) {
                    if (path.isAbsolute()) {
                        file = path.toFile();
                    } else {
                        String currentWorkingDir = System.getProperty("user.dir");
                        Path currentWorkingPath = Paths.get(currentWorkingDir);
                        Path resolvedPath = currentWorkingPath.resolve(path);

                        File aFile = resolvedPath.toFile();
                        if (aFile.canRead()) {
                            file = aFile;
                        } else {
                            // Relative path can exist on multiple root file systems. Try all of them.
                            for (File root : File.listRoots()) {
                                resolvedPath = root.toPath().resolve(path);
                                aFile = resolvedPath.toFile();
                                if (aFile.canRead()) {
                                    file = aFile;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            // Silently swallow the exceptions and try to find the file on the File System
            log.debug(String.format("File [%s] can't be found as file system resource. Exception was caught with the following message: [%s]", fileName, t.getMessage()));
        }

        return file;
    }

}
