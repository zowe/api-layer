/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.product.gateway;

/**
 * Exception thrown when an API Gateway Service is not accessible or found
 */
public class GatewayNotAvailableException extends RuntimeException {

    public GatewayNotAvailableException(String message) {
        super(message);
    }

    public GatewayNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
