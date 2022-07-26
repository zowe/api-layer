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

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

@Slf4j
public class SafResourceAccessSaf implements SafResourceAccessVerifying {

    private static final String PLATFORM_ACCESS_CONTROL_CLASS_NAME = "com.ibm.os390.security.PlatformAccessControl";
    private static final String PLATFORM_PLATFORM_CLASS_NAME = "com.ibm.os390.security.PlatformReturned";
    private static final String CHECK_PERMISSION_METHOD_NAME = "checkPermission";

    private PlatformReturnedHelper<Object> platformReturnedHelper;
    private MethodHandle checkPermission;

    public SafResourceAccessSaf() throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
        init();
    }

    protected Class<?> getPlatformClass() throws ClassNotFoundException {
        return Class.forName(PLATFORM_ACCESS_CONTROL_CLASS_NAME);
    }

    protected Class<?> getPlatformReturnedClass() throws ClassNotFoundException {
        return Class.forName(PLATFORM_PLATFORM_CLASS_NAME);
    }

    protected MethodHandle getCheckPermissionMethodHandle(Class<?> clazz) throws IllegalAccessException, NoSuchMethodException {
        Method method = clazz.getDeclaredMethod(CHECK_PERMISSION_METHOD_NAME, String.class, String.class, String.class, int.class);
        return MethodHandles.lookup().unreflect(method);
    }

    private void init() throws ClassNotFoundException, IllegalAccessException, NoSuchMethodException, NoSuchFieldException {
        platformReturnedHelper = new PlatformReturnedHelper<>((Class<Object>) getPlatformReturnedClass());
        checkPermission = getCheckPermissionMethodHandle(getPlatformClass());
    }

    private boolean evaluatePlatformReturned(PlatformReturned returned, boolean resourceHasToExist) {
        if (returned == null) {
            return true;
        }

        String message;
        PlatformAckErrno errno = PlatformAckErrno.valueOfErrno(returned.getErrno());
        PlatformErrno2 errno2 = PlatformErrno2.valueOfErrno(returned.getErrno2());
        if ((errno == null) || (errno2 == null)) {
            message = "Unknown access control error";
            log.error("Platform access control failed: {}", returned);
        } else {
            message = "Platform access control failed: " + errno2.explanation;
            switch (errno2) {
                case JRSAFResourceUndefined:
                    return !resourceHasToExist;
                case JRSAFNoUser:
                    // When the user is not defined RACF returns JRSAFNoUser but TSS returns JRSAFResourceUndefined.
                case JRNoResourceAccess:
                    return false;
                default:
                    log.error("Platform access control failed: {} {} {} {}",
                        errno.shortErrorName, errno2.shortErrorName, errno2.explanation, returned);
            }
        }
        throw new AccessControlError(returned, message + ": " + returned.toString());
    }

    private PlatformReturned checkPermission(String userId, String resourceType, String resourceName, int accessLevel) {
        try {
            Object platformReturned = checkPermission.invokeWithArguments(userId, resourceType, resourceName, accessLevel);
            return platformReturnedHelper.convert(platformReturned);
        } catch (RuntimeException re) {
            throw re;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    private boolean checkPermission(String userId, String resourceType, String resourceName, int accessLevel, boolean resourceHasToExist) {
        PlatformReturned platformReturned = checkPermission(userId, resourceType, resourceName, accessLevel);
        return evaluatePlatformReturned(platformReturned, resourceHasToExist);
    }

    @Override
    public boolean hasSafResourceAccess(Authentication authentication, String resourceClass, String resourceName, String accessLevel) {
        String userid = authentication.getName();
        AccessLevel level = AccessLevel.valueOf(accessLevel);
        log.debug("Evaluating access of user {} to resource {} in class {} level {}", userid, resourceClass, resourceName, level);
        return checkPermission(userid, resourceClass, resourceName, level.getValue(), true);
    }

}
