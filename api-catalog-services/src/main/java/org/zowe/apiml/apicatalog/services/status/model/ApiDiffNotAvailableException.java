/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.services.status.model;

/**
 * Exception thrown when API diff is not available
 */
public class ApiDiffNotAvailableException extends RuntimeException {

    private static final long serialVersionUID = -7445346342573348213L;

    public ApiDiffNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApiDiffNotAvailableException(String s) {
        super(s);
    }
}
