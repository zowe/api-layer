/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.utils;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@UtilityClass
public class JsonReaderUtil {

    public static String getJsonStringFromResource(String resourceName) throws IOException {
        ClassLoader classLoader = JsonReaderUtil.class.getClassLoader();
        File file = new File(classLoader.getResource(resourceName).getFile());
        return new String(Files.readAllBytes(file.toPath()));
    }

}
