/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.product.instance.lookup;

/**
 * Exception thrown when retryable logic does not succeed to produce desired result
 * When this exception is thrown and handled, the logic is expected to be retried
 */
public class RetryException extends RuntimeException {

    private static final long serialVersionUID = -559112794280136165L;

    public RetryException(String message) {
        super(message);
    }
}
