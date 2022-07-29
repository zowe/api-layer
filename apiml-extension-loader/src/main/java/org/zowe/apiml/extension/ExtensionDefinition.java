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

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Definition based on https://docs.zowe.org/stable/extend/packaging-zos-extensions/#zowe-component-manifest
 */
@Data
@NoArgsConstructor
public class ExtensionDefinition {

    private String name;
    private ApimlServices apimlServices;

    @Data
    @NoArgsConstructor
    public static class ApimlServices {

        private String basePackage;

    }
}
