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

/**
 * Appender which is conditional upon the file logging being enabled (apiml.logging.toFile.enabled) and provided location to store the files in.
 * The conditionality is checked on the start of the Appender to limit the overhead.
 */
public class ApimlRollingFileAppender<E> extends RollingFileAppender<E> { // NOSONAR

    private static final String LOG_TO_FILE_CONTEXT_PROPERTY = "LOG_TO_FILE";
    private static final String LOG_TO_FILE_PROPERTY = "apiml.logging.toFile.enabled";

    @Override
    public void start() {
        if (verifyStartupParams()) {
            super.start();
        }
    }

    /**
     * Verifies that the file logging is enabled and that there is a location to use within the zowe instance.
     * The enabled flag is taken from the Logback context (bound from the Spring property by logback-spring.xml).
     * @return true if everything is ok, false otherwise.
     */
    protected boolean verifyStartupParams() {
        String enabled = getContext() != null ? getContext().getProperty(LOG_TO_FILE_CONTEXT_PROPERTY) : null;
        if (!Boolean.parseBoolean(enabled)) {
            addInfo("Logging to file isn't enabled (" + LOG_TO_FILE_PROPERTY + "). File appender will be disabled.");
            return false;
        }

        String location = System.getProperty("apiml.logs.location");
        if (location == null || location.isEmpty()) {
            addWarn("The WORKSPACE_DIR must be set to store logs.");
            return false;
        }

        return true;
    }
}
