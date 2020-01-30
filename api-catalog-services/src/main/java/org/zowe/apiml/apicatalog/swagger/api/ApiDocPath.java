/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.swagger.api;

import lombok.Getter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
public class ApiDocPath<T> {
    private Map<String, T> shortPaths;
    private Map<String, T> longPaths;
    private Set<String> prefixes;

    public ApiDocPath() {
        shortPaths = new HashMap<>();
        longPaths = new HashMap<>();
        prefixes = new HashSet<>();
    }

    public void addPrefix(String prefix) {
        getPrefixes().add(prefix);
    }

    public void addShortPath(String name, T path) {
        getShortPaths().put(name, path);
    }

    public void addLongPath(String name, T path) {
        getLongPaths().put(name, path);
    }
}
