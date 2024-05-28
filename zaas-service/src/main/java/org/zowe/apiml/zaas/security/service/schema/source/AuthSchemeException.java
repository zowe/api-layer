/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.schema.source;

public class AuthSchemeException extends RuntimeException {
    final String[] params;

    public AuthSchemeException(String message) {
        super(message);
        params = null;
    }

    public AuthSchemeException(String message, String ... params) {
        super(message);
        this.params = params;
    }

    public String[] getParams() { return params; }
}
