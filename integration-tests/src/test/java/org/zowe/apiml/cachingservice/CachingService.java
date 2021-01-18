/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.cachingservice;

import java.io.IOException;
import java.util.ArrayList;

public class CachingService {
    private final String id = "test";
    private Process newCachingProcess;

    public void start() throws IOException {
        if (newCachingProcess != null) {
            newCachingProcess.destroy();
        }

        ArrayList<String> discoveryCommand = new ArrayList<>();
        discoveryCommand.add("java");
        discoveryCommand.add("-jar");
        discoveryCommand.add("../eureka-discovery/build/libs/eureka-discovery.jar");

        ProcessBuilder builder1 = new ProcessBuilder(discoveryCommand);
        newCachingProcess = builder1.inheritIO().start();
    }
}
