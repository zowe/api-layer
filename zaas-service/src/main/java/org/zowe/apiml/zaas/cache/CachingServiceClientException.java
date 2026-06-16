/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.cache;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

public class CachingServiceClientException extends RuntimeException {

    public CachingServiceClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public CachingServiceClientException(String message) {
        super(message);
    }

    public boolean isKeyCollision() {
        return getCause() instanceof HttpStatusCodeException httpException
            && HttpStatus.CONFLICT.equals(httpException.getStatusCode());
    }
}
