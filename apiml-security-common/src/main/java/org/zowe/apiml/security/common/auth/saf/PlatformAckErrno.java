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

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides explanation for error codes for authentication as described at
 * documentation for BPX4ACK:
 * https://www.ibm.com/support/knowledgecenter/SSLTBW_2.4.0/com.ibm.zos.v2r4.bpxb100/ack.htm
 */
@RequiredArgsConstructor
public enum PlatformAckErrno {

    EACCES("EACCES", 111, "Permission is denied; the specified password is incorrect", PlatformErrorType.DEFAULT),
    EINVAL("EINVAL", 121, "Invalid input parameters", PlatformErrorType.DEFAULT),
    EMVSSAF2ERR("EMVSSAF2ERR", 164, "An error occurred in the security product", PlatformErrorType.INTERNAL),
    ENOSYS("ENOSYS", 134, "The function is not supported on this system", PlatformErrorType.INTERNAL),
    EPERM("EPERM", 139, "The user does not have the access specified to the resource or the calling address space is not authorized to use this service or a load from a not program-controlled library was done in the address space", PlatformErrorType.ERRNO2_REQUIRED),
    ESRCH("ESRCH", 143, "The user ID or resource is not defined to the security product", PlatformErrorType.ERRNO2_REQUIRED);

    private static final Map<Integer, PlatformAckErrno> BY_ERRNO = new HashMap<>();

    static {
        for (PlatformAckErrno e : values()) {
            BY_ERRNO.put(e.errno, e);
        }
    }

    public final String shortErrorName;
    public final int errno;
    public final String explanation;
    public final PlatformErrorType errorType;

    public static PlatformAckErrno valueOfErrno(int errno) {
        return BY_ERRNO.getOrDefault(errno, null);
    }

}
