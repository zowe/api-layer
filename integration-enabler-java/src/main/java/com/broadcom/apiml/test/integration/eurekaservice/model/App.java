/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.test.integration.eurekaservice.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName(value = "app")
public class App {
    private String name;
    private String description;
    private String version;

    public App(String name, String description, String version) {
        this.name = name;
        this.description = description;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getVersion() {
        return version;
    }
}
