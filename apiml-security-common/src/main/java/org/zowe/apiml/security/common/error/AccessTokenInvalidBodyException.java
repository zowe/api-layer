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

import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

/**
 * Exception thrown when the request body for Personal Access Token creation is not valid
 */
@Getter
public class AccessTokenInvalidBodyException extends AuthenticationException {

    private static final long serialVersionUID = 2846297491901169989L;

    public AccessTokenInvalidBodyException(String message) {
        super(message);
    }
}
