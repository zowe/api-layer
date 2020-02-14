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

import java.util.*;
import java.util.function.Consumer;

public class MapUtils {

    public Map<String, String> flattenMap(String rootKey, Map<String, Object> collection) {
        if (collection == null || collection.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new HashMap<>();

        Map<Class, Consumer<Map.Entry<String, Object>>> actionMap = new LinkedHashMap<>();
        actionMap.put(Map.class, entry -> result.putAll(flattenMap(mergeKey(rootKey, entry.getKey()), (Map<String, Object>)entry.getValue())));
        actionMap.put(String.class, entry -> result.put(mergeKey(rootKey, entry.getKey()), entry.getValue().toString()));
        actionMap.put(Boolean.class, entry -> result.put(mergeKey(rootKey, entry.getKey()), entry.getValue().toString()));
        actionMap.put(Integer.class, entry -> result.put(mergeKey(rootKey, entry.getKey()), entry.getValue().toString()));
        actionMap.put(Double.class, entry -> result.put(mergeKey(rootKey, entry.getKey()), entry.getValue().toString()));
        actionMap.put(Float.class, entry -> result.put(mergeKey(rootKey, entry.getKey()), entry.getValue().toString()));
        actionMap.put(List.class, entry -> { throw new IllegalArgumentException("List parsing is not supported"); } );
        actionMap.put(Object[].class, entry -> { throw new IllegalArgumentException("Array parsing is not supported"); } );
        actionMap.put(Object.class, entry -> { throw new IllegalArgumentException(String.format("Cannot parse key: %s with value %s", entry.getKey(), entry.getValue().toString())); } );

        for (Map.Entry<String, Object> entry : collection.entrySet()) {
            if (entry.getValue() == null) {
                result.put( mergeKey(rootKey, entry.getKey()), "");
                continue;
            }
            executeAction(entry,actionMap);
        }
        return result;
    }

    private void executeAction(Map.Entry<String, Object> switchSubject, Map<Class, Consumer<Map.Entry<String, Object>>> actionMap) {
        for (Map.Entry<Class, Consumer<Map.Entry<String, Object>>> action : actionMap.entrySet()) {
            if (action.getKey().isInstance(switchSubject.getValue())) {
                action.getValue().accept(switchSubject);
                break;
            }
        }
    }

    private String mergeKey(String rootKey, String newKey) {
        return rootKey != null ? rootKey + "." + newKey : newKey;
    }
}
