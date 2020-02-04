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
 * Exception thrown when API Doc is not enabled
 */
public class ApiDocNotEnabledException extends RuntimeException {


    private static final long serialVersionUID = -3062545981085439271L;

    /**
     * Api Doc exception
     * @param s message
     */
    public ApiDocNotEnabledException(String s) {
        super(s);
    }

    /**
     * Do not print a stack trace for this exception
     * @return
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
