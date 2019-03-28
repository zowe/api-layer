/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.product.host;


/**
 * Exception thrown when a protocol is not http or https
 */
public class InvalidProtocolException extends RuntimeException {

    public InvalidProtocolException(String s) {
        super(s);
    }

    public InvalidProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
