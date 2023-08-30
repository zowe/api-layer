/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.conformance;

import lombok.Getter;

public class ValidationException extends RuntimeException {

    @Getter
    private final String key;

    public ValidationException(String msg, String key) {
        super(msg);
        this.key = key;
    }
}
