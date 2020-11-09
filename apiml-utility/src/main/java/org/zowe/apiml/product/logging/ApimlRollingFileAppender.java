/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.product.logging;

import ch.qos.logback.core.rolling.RollingFileAppender;

public class ApimlRollingFileAppender extends RollingFileAppender {
    public void start() {
        String debug = System.getenv("LOG_LEVEL");
        if (debug == null || !debug.equalsIgnoreCase("DEBUG")) {
            addInfo("The level isn't set to debug. File appender will be disabled.");
            return;
        }

        String location = System.getenv("WORKSPACE_DIR");
        if(location == null) {
            addWarn("The WORKSPACE_DIR must be set to store logs.");
            return;
        }

        super.start();
    }
}
