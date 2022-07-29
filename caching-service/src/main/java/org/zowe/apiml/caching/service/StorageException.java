/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.service;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class StorageException extends RuntimeException {
    private final String key;
    private final String[] parameters;
    private final HttpStatus status;

    public StorageException(String key, HttpStatus status, String... messageParameters) {
        super(key);

        this.key = key;
        this.status = status;
        this.parameters = messageParameters;
    }

    public StorageException(String key, HttpStatus status, Exception cause, String... messageParameters) {
        super(key, cause);

        this.key = key;
        this.status = status;
        this.parameters = messageParameters;
    }
}
