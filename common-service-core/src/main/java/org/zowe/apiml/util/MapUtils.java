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

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

import java.util.*;
import java.util.function.Consumer;

@UtilityClass
public class MapUtils {

    @Getter
    @RequiredArgsConstructor
    private static final class Argument {
        @NonNull
        private final Map.Entry<String, Object> entry;
        @NonNull
        private final Map<String, String> result;
        private final String rootKey;
    }

    private static final Consumer<Argument> MAP_CONSUMER = argument -> argument.getResult().putAll(flattenMap(mergeKey(argument.getRootKey(), argument.getEntry().getKey()), (Map<String, Object>) argument.getEntry().getValue()));
    private static final Consumer<Argument> PRIMITIVE_CONSUMER = argument -> argument.getResult().put(mergeKey(argument.getRootKey(), argument.getEntry().getKey()), argument.getEntry().getValue().toString());
    private static final Map<Class, Consumer<Argument>> ACTION_MAP = new LinkedHashMap<>();

    static {
        ACTION_MAP.put(Map.class, MAP_CONSUMER);
        ACTION_MAP.put(String.class, PRIMITIVE_CONSUMER);
        ACTION_MAP.put(Boolean.class, PRIMITIVE_CONSUMER);
        ACTION_MAP.put(Integer.class, PRIMITIVE_CONSUMER);
        ACTION_MAP.put(Double.class, PRIMITIVE_CONSUMER);
        ACTION_MAP.put(Float.class, PRIMITIVE_CONSUMER);
        ACTION_MAP.put(List.class, argument -> { throw new IllegalArgumentException("List parsing is not supported"); } );
        ACTION_MAP.put(Object[].class, argument -> { throw new IllegalArgumentException("Array parsing is not supported"); } );
        ACTION_MAP.put(Object.class, argument -> { throw new IllegalArgumentException(String.format("Cannot parse key: %s with value %s", argument.getEntry().getKey(), argument.getEntry().getValue().toString())); } );
    }

    public static Map<String, String> flattenMap(String rootKey, Map<String, Object> collection) {
        if (collection == null || collection.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new HashMap<>();

        for (Map.Entry<String, Object> entry : collection.entrySet()) {
            if (entry.getValue() == null) {
                result.put( mergeKey(rootKey, entry.getKey()), "");
                continue;
            }
            executeAction(new Argument(entry, result, rootKey));
        }
        return result;
    }

    private static void executeAction(Argument argument) {

        for (Map.Entry<Class, Consumer<Argument>> action : ACTION_MAP.entrySet()) {
            if (action.getKey().isInstance(argument.getEntry().getValue())) {
                action.getValue().accept(argument);
                break;
            }
        }
    }

    private static String mergeKey(String rootKey, String newKey) {
        return rootKey != null ? rootKey + "." + newKey : newKey;
    }
}
