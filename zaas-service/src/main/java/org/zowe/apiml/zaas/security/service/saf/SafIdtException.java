/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.saf;

import org.springframework.security.access.AccessDeniedException;

public class SafIdtException extends AccessDeniedException {

    private static final long serialVersionUID = 8144117709741703975L;

    public SafIdtException(String message, Throwable cause) {
        super(message, cause);
    }

    public SafIdtException(String message) {
        super(message);
    }

}