/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

public class InvalidForwardException extends RuntimeException {
    
    private static final long serialVersionUID = 2717623179179277677L;

    public InvalidForwardException(String message) {
        super(message);
    }
    public InvalidForwardException(String message, Throwable cause) {
        super(message, cause);
    }
}
