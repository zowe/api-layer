/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.auth.saf;

import java.lang.reflect.Field;

/**
 * This class prepare fast conversion from mainframe class into PlatformReturned. It avoid to have mainframe class
 * in the classpath (independent from mainframe using java reflection) and precalculate for the fastest conversion.
 *
 * If a class does not match with expected class (set of fields), the constructor throws exception about.
 */
public class PlatformReturnedHelper<T> {

    private static final String STRUCTURE_CHANGE_MSG = "Unknown structure of PlatformReturned class";

    private final Field successField;
    private final Field rcField;
    private final Field errnoField;
    private final Field errno2Field;
    private final Field errnoMsgField;
    private final Field stringRetField;
    private final Field objectRetField;

    /**
     * Prepare conversion for class platformReturnedClass into PlatformReturned
     * @param platformReturnedClass Class to me converted
     * @throws NoSuchFieldException If structure is not matching
     */
    public PlatformReturnedHelper(Class<T> platformReturnedClass) throws NoSuchFieldException {
        successField = platformReturnedClass.getDeclaredField("success");
        rcField = platformReturnedClass.getDeclaredField("rc");
        errnoField = platformReturnedClass.getDeclaredField("errno");
        errno2Field = platformReturnedClass.getDeclaredField("errno2");
        errnoMsgField = platformReturnedClass.getDeclaredField("errnoMsg");
        stringRetField = platformReturnedClass.getDeclaredField("stringRet");
        objectRetField = platformReturnedClass.getDeclaredField("objectRet");
    }

    /**
     * Convert original DTO class into PlatformReturned
     * @param o original object
     * @return PlatformReturned with values from original one, whn o is null, null is returned
     * @throws IllegalArgumentException if type of a field is not matching
     */
    public PlatformReturned convert(T o) {
        if (o == null) return null;

        try {
            return PlatformReturned.builder()
                .success(successField.getBoolean(o))
                .rc(rcField.getInt(o))
                .errno(errnoField.getInt(o))
                .errno2(errno2Field.getInt(o))
                .errnoMsg((String) errnoMsgField.get(o))
                .stringRet((String) stringRetField.get(o))
                .objectRet(objectRetField.get(o))
                .build();
        } catch (IllegalAccessException iae) {
            throw new IllegalArgumentException(STRUCTURE_CHANGE_MSG);
        }
    }

}
