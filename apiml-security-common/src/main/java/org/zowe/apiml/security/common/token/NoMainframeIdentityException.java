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
 * This exception is thrown in case the OIDC token is valid but distributed ID is not mapped to the mainframe ID.
 */
public class NoMainframeIdentityException extends AuthenticationException {
    public NoMainframeIdentityException(String msg) {
        super(msg);
    }
}
