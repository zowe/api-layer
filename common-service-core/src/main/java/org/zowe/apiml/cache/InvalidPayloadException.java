/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cache;

import org.springframework.http.HttpStatus;

public class InvalidPayloadException extends StorageException {
    public InvalidPayloadException(String key, String... messageParameters) {
        super(key, HttpStatus.BAD_REQUEST, messageParameters);
    }
    public InvalidPayloadException(String key, Exception cause, String... messageParameters) {
        super(key, HttpStatus.BAD_REQUEST, cause, messageParameters);
    }
}
