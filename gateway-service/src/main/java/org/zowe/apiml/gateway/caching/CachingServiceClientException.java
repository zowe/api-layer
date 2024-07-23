/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.caching;

import lombok.Getter;

@Getter
public class CachingServiceClientException extends RuntimeException {

    private final int statusCode;

    public CachingServiceClientException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

}
