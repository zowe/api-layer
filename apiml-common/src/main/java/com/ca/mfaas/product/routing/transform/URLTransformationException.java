/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.product.routing.transform;

/**
 * Exception used to indicate that URL couldn't be transformable.
 */
public class URLTransformationException extends Exception {

    public URLTransformationException(String message) {
        super(message);
    }

    public URLTransformationException(String message, Throwable cause) {
        super(message, cause);
    }
}
