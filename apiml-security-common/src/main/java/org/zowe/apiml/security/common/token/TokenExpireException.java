/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.security.common.token;

import org.springframework.security.core.AuthenticationException;

/**
 * This exception is thrown in case the JWT token is expired.
 */
public class TokenExpireException extends AuthenticationException {

    public TokenExpireException(String msg) {
        super(msg);
    }

    public TokenExpireException(String msg, Throwable t) {
        super(msg, t);
    }
}
