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

public class SafPlatformUser implements PlatformUser {
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
                return PlatformReturned.builder().success(returnedClass.getField("success").getBoolean(safReturned))
                    .rc(returnedClass.getField("rc").getInt(safReturned))
                    .errno(returnedClass.getField("errno").getInt(safReturned))
                    .errno2(returnedClass.getField("errno2").getInt(safReturned))
                    .errnoMsg((String) returnedClass.getField("errnoMsg").get(safReturned))
                    .stringRet((String) returnedClass.getField("stringRet").get(safReturned))
                    .objectRet(returnedClass.getField("objectRet").get(safReturned)).build();
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
            | SecurityException | ClassNotFoundException | NoSuchFieldException e) {
            throw new AuthenticationServiceException("A failure occurred when authenticating.", e);
        }
    }

}
