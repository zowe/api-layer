/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.saf;

import org.springframework.security.core.AuthenticationException;

public class SafIdtAuthException extends AuthenticationException {

    private static final long serialVersionUID = 8654672736072633160L;

    public SafIdtAuthException(String message, Throwable cause) {
        super(message, cause);
    }

    public SafIdtAuthException(String message) {
        super(message);
    }

}