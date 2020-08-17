/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.login.saf;

import org.springframework.security.authentication.AuthenticationServiceException;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public class SafPlatformUser implements PlatformUser {
    public static final String SUCCESS = "success";
    public static final String RC = "rc";
    public static final String ERRNO = "errno";
    public static final String ERRNO_2 = "errno2";
    public static final String ERRNO_MSG = "errnoMsg";
    public static final String STRING_RET = "stringRet";
    public static final String OBJECT_RET = "objectRet";
    private final PlatformClassFactory platformClassFactory;

    public SafPlatformUser(PlatformClassFactory platformClassFactory) {
        this.platformClassFactory = platformClassFactory;
    }

    @Override
    public PlatformReturned authenticate(String userid, String password) {
        try {
            Object safReturned = platformClassFactory.getPlatformUserClass()
                    .getMethod("authenticate", String.class, String.class)
                    .invoke(platformClassFactory.getPlatformUser(), userid, password);
            if (safReturned == null) {
                return null;
            } else {
                Class<?> returnedClass = platformClassFactory.getPlatformReturnedClass();
                return PlatformReturned.builder().success(doesObjectContainField(safReturned, SUCCESS) && returnedClass.getField(SUCCESS).getBoolean(safReturned))
                        .rc(doesObjectContainField(safReturned, RC) ? returnedClass.getField(RC).getInt(safReturned) : -1)
                        .errno(doesObjectContainField(safReturned, ERRNO) ? returnedClass.getField(ERRNO).getInt(safReturned) : -1)
                        .errno2(doesObjectContainField(safReturned, ERRNO_2) ? returnedClass.getField(ERRNO_2).getInt(safReturned) : -1)
                        .errnoMsg(doesObjectContainField(safReturned, ERRNO_MSG) ? (String) returnedClass.getField(ERRNO_MSG).get(safReturned) : "N/A")
                        .stringRet(doesObjectContainField(safReturned, STRING_RET) ? (String) returnedClass.getField(STRING_RET).get(safReturned) : "N/A")
                        .objectRet(doesObjectContainField(safReturned, OBJECT_RET) ? returnedClass.getField(OBJECT_RET).get(safReturned) : "N/A").build();
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                | SecurityException | ClassNotFoundException | NoSuchFieldException e) {
            throw new AuthenticationServiceException("A failure occurred when authenticating.", e);
        }
    }

    private boolean doesObjectContainField(Object object, String fieldName) {
        return Arrays.stream(object.getClass().getFields())
            .anyMatch(field -> field.getName().equals(fieldName));
    }

}
