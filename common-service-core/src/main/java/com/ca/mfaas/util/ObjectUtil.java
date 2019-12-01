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
import java.util.Set;

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

    /**
     *
     * @return the class object, from which this function was called
     */
    public static Class getThisClass() {
        Thread theThread = Thread.currentThread();
        StackTraceElement[]  stackTrace = theThread.getStackTrace();
        String theClassName = stackTrace[2].getClassName();
        Class theClass = null;
        try {
            theClass = Class.forName(theClassName);
        } catch (ClassNotFoundException cnfe) {
            log.error(String.format("Class %s was not found: ", theClassName), cnfe);
        }
        return theClass;
    }

    /**
      *  Deep merge of two maps. Drills down recursively into Container values
      */
    public static Map<String, Object> mergeMapsDeep(Map map1, Map map2) {
        for (Map.Entry<String, Object> entry : (Set<Map.Entry>)map2.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (map1.get(key) instanceof Map && value instanceof Map) {
                map1.put(key, mergeMapsDeep((Map) map1.get(key), (Map)value));
            } else if (map1.get(key) instanceof List && value instanceof List) {
                List originalChild = (List) map1.get(key);
                for (Object each : (List)value) {
                    if (!originalChild.contains(each)) {
                        originalChild.add(each);
                    }
                }
            } else {
                map1.put(key, value);
            }
        }
        return map1;
    }
}
