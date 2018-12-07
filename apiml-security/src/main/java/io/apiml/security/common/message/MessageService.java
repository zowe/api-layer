/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package io.apiml.security.common.message;

import org.springframework.stereotype.Service;

@Service
public class MessageService {
    public ApiMessage createError(int status, String code, String message, String traceId) {
        return ApiErrorResponse.builder()
            .error(Error.builder()
                    .status(status)
                    .code(code)
                    .message(message)
                    .traceId(traceId)
                    .build()
            ).build();
    }
}
