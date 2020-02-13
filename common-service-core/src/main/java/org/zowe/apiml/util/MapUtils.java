/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapUtils {

    public Map<String, String> flattenMap(String rootKey, Map<String, Object> collection) {
        if (collection == null || collection.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new HashMap<>();

        for (Map.Entry<String, Object> entry : collection.entrySet()) {

            if (entry.getValue() == null) {
                result.put( mergeKey(rootKey, entry.getKey()), "");
                continue;
            }


            if (entry.getValue() instanceof Map) {
                result.putAll(flattenMap(mergeKey(rootKey, entry.getKey()), (Map<String, Object>)entry.getValue()));
                continue;
            }

            if (entry.getValue() instanceof String ||
                entry.getValue() instanceof Boolean ||
                entry.getValue() instanceof Integer ||
                entry.getValue() instanceof Double ||
                entry.getValue() instanceof Float) {
                result.put(mergeKey(rootKey, entry.getKey()), entry.getValue().toString());
                continue;
            }

            if (entry.getValue() instanceof List) {
                throw new IllegalArgumentException("List parsing is not supported");
            }
            if (entry.getValue().getClass().isArray()) {
                throw new IllegalArgumentException("Array parsing is not supported");
            }
            throw new IllegalArgumentException(String.format("Cannot parse key: %s with value %s", entry.getKey(), entry.getValue().toString()));
        }

        return result;
    }


    private String mergeKey(String rootKey, String newKey) {
        return rootKey != null ? rootKey + "." + newKey : newKey;
    }
}
