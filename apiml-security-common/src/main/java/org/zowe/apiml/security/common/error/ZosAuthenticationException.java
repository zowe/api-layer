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

import org.springframework.security.core.AuthenticationException;
import org.zowe.apiml.security.common.auth.saf.PlatformReturned;

public class ZosAuthenticationException extends AuthenticationException {

    protected final PlatformPwdErrno platformPwdErrno;

    private static final long serialVersionUID = 6652673387938170807L;

    public ZosAuthenticationException(PlatformReturned platformReturned) {
        super("z/OS Authentication exception");
        if (platformReturned == null) {
            throw new IllegalArgumentException("PlatformReturned must not be null");
        }
        this.platformPwdErrno = PlatformPwdErrno.valueOfErrno(platformReturned.getErrno());
    }

    @Override
    public String getMessage() {
        return platformPwdErrno.shortErrorName + ": " + platformPwdErrno.explanation;
    }

    public PlatformPwdErrno getPlatformError() {
        return platformPwdErrno;
    }
}
