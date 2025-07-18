/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.log.ApimlLogger;

import java.io.IOException;
import java.util.function.BiConsumer;

@UtilityClass
@Slf4j
public class ServletErrorUtils {

    public static BiConsumer<ApiMessageView, HttpStatus> createApiErrorWriter(HttpServletResponse response, ApimlLogger logger) {
        return (apiMessageView, status) -> {
            response.setStatus(status.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            try {
                var mapper = new ObjectMapper();
                mapper.writeValue(response.getWriter(), apiMessageView);
            } catch (IOException e) {
                if (!response.isCommitted()) {
                    log.debug("Failed writing content to not-commited response", e);
                } else {
                    logger.log(MessageType.DEBUG, "Response already committed. Skipping error write log.");
                }
            }
        };
    }
}
