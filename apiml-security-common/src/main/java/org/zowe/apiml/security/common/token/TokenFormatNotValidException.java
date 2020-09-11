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
 * This exception is thrown in case the JWT token format provided during logout is not valid.
 */
public class TokenFormatNotValidException extends AuthenticationException {

    public TokenFormatNotValidException(String msg) {
        super(msg);
    }

    public TokenFormatNotValidException(String msg, Throwable t) {
        super(msg, t);
    }
}
