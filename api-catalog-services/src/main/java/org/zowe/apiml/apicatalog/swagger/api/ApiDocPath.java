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

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

@Getter
public class ApiDocPath<T> {
    private final Map<String, T> shortPaths = new TreeMap<>();
    private final Map<String, T> longPaths = new TreeMap<>();
    private final Set<String> prefixes = new TreeSet<>();

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
