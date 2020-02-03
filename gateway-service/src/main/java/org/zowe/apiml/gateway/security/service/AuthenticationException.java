/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service;

public class AuthenticationException extends Exception {
    private static final long serialVersionUID = -5152411541425940337L;

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
