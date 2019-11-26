/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@UtilityClass
public class ObjectUtil {

    /**
     * Check whether the specified object reference is not null and
     * throws a {@link IllegalArgumentException} if it is.
     *
     * @param param   the object reference to check for nullity
     * @param message detail message to be used in the event
     */
    public static void requireNotNull(Object param, String message) {
        if (param == null) {
            throw new IllegalArgumentException(message);
        }
    }

    public static Class getThisClass() {
        Thread theThread = Thread.currentThread();
        StackTraceElement[]  stackTrace = theThread.getStackTrace();
        String theClassName = stackTrace[2].getClassName();
        try {
            Class theClass = Class.forName(theClassName);
            return theClass;
        } catch (ClassNotFoundException cnfe) {
            log.error(String.format("Calss {} was not found: ", theClassName) + cnfe);
        }
        return null;
    }

    /**
      *  Deep merge of two maps. Drills down recursively into Container values
      */
    public static Map mergeMapsDeep(Map map1, Map map2) {
        for (Object key : map2.keySet()) {
            if (map2.get(key) instanceof Map && map1.get(key) instanceof Map) {
                Map originalChild = (Map) map1.get(key);
                Map newChild = (Map) map2.get(key);
                map1.put(key, mergeMapsDeep(originalChild, newChild));
            } else if (map2.get(key) instanceof List && map1.get(key) instanceof List) {
                List originalChild = (List) map1.get(key);
                List newChild = (List) map2.get(key);
                for (Object each : newChild) {
                    if (!originalChild.contains(each)) {
                        originalChild.add(each);
                    }
                }
            } else {
                map1.put(key, map2.get(key));
            }
        }
        return map1;
    }

    /**
     * @Experimental
     *
     * @param map1
     * @param map2
     * @return
     */
    public static Map <String, Object> mergeMaps(Map<String, Object> map1, Map<String, Object> map2) {

        Map<String, Object> resultMap = Stream.concat(map1.entrySet().stream(), map2.entrySet().stream())
            .collect(Collectors.toMap(
                entry -> entry.getKey() // The key
                , entry -> entry.getValue() // The value
                // The "merger"
                , (entry1, entry2) -> { return mergeMapEntries(entry1, entry2); }
                )
            );

        return resultMap;
    }

    /**
     * @Experimental
     *
     * @param entry1
     * @param entry2
     * @return
     */
    public static Object mergeMapEntries(Object entry1, Object entry2) {
        if (entry2 == null) {
            return  entry1;
        }

        if (entry2 instanceof Map) {
            return entry1;
        } else {
            return entry2;
        }
    }
}
