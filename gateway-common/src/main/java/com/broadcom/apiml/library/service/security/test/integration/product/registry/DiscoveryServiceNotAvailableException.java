/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.security.test.integration.product.registry;

public class DiscoveryServiceNotAvailableException extends RuntimeException {

    public DiscoveryServiceNotAvailableException(String message) {
        super(message);
    }

    public DiscoveryServiceNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
