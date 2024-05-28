/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.sse;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum SseErrorMessages {
    INVALID_ROUTE("org.zowe.apiml.zaas.invalidRoute", HttpStatus.BAD_REQUEST),
    INSTANCE_NOT_FOUND("org.zowe.apiml.zaas.instanceNotFound", HttpStatus.NOT_FOUND),
    ENDPOINT_NOT_FOUND("org.zowe.apiml.common.endPointNotFound", HttpStatus.NOT_FOUND);
    private final String key;
    private final HttpStatus status;
}
