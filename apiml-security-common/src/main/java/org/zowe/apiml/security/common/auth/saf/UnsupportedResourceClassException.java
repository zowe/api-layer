/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.auth.saf;

import lombok.Getter;

public class UnsupportedResourceClassException extends RuntimeException {

    private static final long serialVersionUID = -370307124912804739L;

    @Getter
    private final String resourceClass;

    public UnsupportedResourceClassException(String resourceClass, String message) {
        super(message);
        this.resourceClass = resourceClass;
    }

}
