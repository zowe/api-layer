/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.services.status.model;

/**
 * Exception thrown when API Doc is not accessible
 */
public class ApiDocNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -3659165931363466710L;

    public ApiDocNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApiDocNotFoundException(String s) {
        super(s);
    }

    /**
     * Do not print a stack trace for this exception
     *
     * @return
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
