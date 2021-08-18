/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.error;

import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides explanation for error codes for authentication as described at
 * documentation for BPX4PWD:
 * https://www.ibm.com/support/knowledgecenter/SSLTBW_2.4.0/com.ibm.zos.v2r4.bpxb100/pwd.htm
 */
@SuppressWarnings("squid:S1192") //literal repeated for readability
public enum PlatformPwdErrno {
    UNKNOWN("UNKNOWN", 0, "Unknown error",
        "org.zowe.apiml.security.platform.errno.UNKNOWN", HttpStatus.INTERNAL_SERVER_ERROR),
    EACCES("EACCES", 111, "Permission is denied; the specified password is incorrect",
        "org.zowe.apiml.security.platform.errno.EACCES", HttpStatus.UNAUTHORIZED),
    EINVAL("EINVAL", 121, "Invalid user name or password is invalid",
        "org.zowe.apiml.security.platform.errno.EINVAL", HttpStatus.UNAUTHORIZED),
    EMVSERR("EMVSERR", 157, "An MVS environmental error has been detected",
        "org.zowe.apiml.security.platform.errno.ERROR", HttpStatus.INTERNAL_SERVER_ERROR),
    EMVSEXPIRE("EMVSEXPIRE", 168, "The password for the specified identity has expired",
        "org.zowe.apiml.security.platform.errno.EMVSEXPIRE", HttpStatus.UNAUTHORIZED),
    EMVSPASSWORD("EMVSPASSWORD", 169, "The new password is not valid",
        "org.zowe.apiml.security.platform.errno.EMVSPASSWORD", HttpStatus.BAD_REQUEST),
    EMVSSAF2ERR("EMVSSAF2ERR", 164, "An error occurred in the security product",
        "org.zowe.apiml.security.platform.errno.ERROR", HttpStatus.UNAUTHORIZED),
    EMVSSAFEXTRERR("EMVSSAFEXTRERR", 163, "The username access has been revoked",
        "org.zowe.apiml.security.platform.errno.EMVSSAFEXTRERR", HttpStatus.UNAUTHORIZED),
    ENOSYS("ENOSYS", 134, "The function is not supported on this system",
        "org.zowe.apiml.security.platform.errno.ERROR", HttpStatus.INTERNAL_SERVER_ERROR),
    EPERM("EPERM", 139, "The calling address space is not authorized to use this service or a load from a not program-controlled library was done in the address space",
        "org.zowe.apiml.security.platform.errno.ERROR", HttpStatus.INTERNAL_SERVER_ERROR),
    ESRCH("ESRCH", 143, "The identity that was specified is not defined to the security product",
        "org.zowe.apiml.security.platform.errno.ESRCH", HttpStatus.UNAUTHORIZED);

    private static final Map<Integer, PlatformPwdErrno> BY_ERRNO = new HashMap<>();

    static {
        for (PlatformPwdErrno e : values()) {
            BY_ERRNO.put(e.errno, e);
        }
    }

    public final String shortErrorName;
    public final int errno;
    public final String explanation;
    public final String errorMessage;
    public final HttpStatus responseCode;

    private PlatformPwdErrno(String shortErrorName, int errno, String explanation, String errorMessage, HttpStatus responseCode) {
        this.shortErrorName = shortErrorName;
        this.errno = errno;
        this.explanation = explanation;
        this.errorMessage = errorMessage;
        this.responseCode = responseCode;
    }

    public static PlatformPwdErrno valueOfErrno(int errno) {
        return BY_ERRNO.getOrDefault(errno, null);
    }
}
