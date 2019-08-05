/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.error;

/**
 * Exception thrown when requested service is not accessible
 */
public class ServiceNotAccessibleException extends RuntimeException {

    public ServiceNotAccessibleException(String message) {
        super(message);
    }

    public ServiceNotAccessibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
