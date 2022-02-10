/*
* This program and the accompanying materials are made available under the terms of the
* Eclipse Public License v2.0 which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Copyright Contributors to the Zowe Project.
*/
package org.zowe.apiml.extension;

import static java.util.Collections.emptyList;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

public class ZoweRuntimeEnvironment {

    static final String INSTALLED_EXTENSIONS_ENV = "ZWE_INSTALLED_COMPONENTS";
    static final String ENABLED_EXTENSIONS_ENV = "ZWE_ENABLED_COMPONENTS";

    static final String WORKSPACE_DIR_ENV = "ZWE_zowe_workspaceDirectory";
    static final String COMPONENTS_APP_SERVER_DIR_ENV = "ZWE_components_app_server_pluginsDir";
    static final String PLUGINS_DIR_ENV = "ZWED_pluginsDir";
    static final String EXTENSION_DIR_ENV = "ZWE_zowe_extensionDirectory";

    ZoweRuntimeEnvironment() {
        super();
    }

    public static ZoweRuntimeEnvironment defaultEnv() {
        return new ZoweRuntimeEnvironment();
    }

    Optional<String> getPluginsDir() {
        String pluginsDir = System.getenv(PLUGINS_DIR_ENV);
        String componentsAppServerPluginsDir = System.getenv(COMPONENTS_APP_SERVER_DIR_ENV);
        String workspaceDir = System.getenv(WORKSPACE_DIR_ENV);

        if (StringUtils.isNotEmpty(pluginsDir)) {
            return Optional.of(pluginsDir);
        } else if (StringUtils.isNotEmpty(componentsAppServerPluginsDir)) {
            return Optional.of(componentsAppServerPluginsDir);
        } else if (StringUtils.isNotEmpty(workspaceDir)) {
            return Optional.of(workspaceDir);
        }
        return Optional.empty();
    }

    List<String> getInstalledComponents() {
        return Optional.ofNullable(System.getenv(INSTALLED_EXTENSIONS_ENV))
                .map(installed -> installed.split(","))
                .map(Arrays::asList)
                .orElse(emptyList());
    }

    List<String> getEnabledComponents() {
        return Optional.ofNullable(System.getenv(ENABLED_EXTENSIONS_ENV))
                .map(enabled -> enabled.split(","))
                .map(Arrays::asList)
                .orElse(emptyList());
    }

    String getExtensionDirecotry() {
        return System.getenv(EXTENSION_DIR_ENV);
    }
}
