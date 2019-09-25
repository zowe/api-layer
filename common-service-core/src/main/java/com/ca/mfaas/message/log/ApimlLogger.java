/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.message.log;

import com.ca.mfaas.message.core.Message;
import com.ca.mfaas.message.core.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ApimlLogger {

    private final MessageService messageService;
    private final Logger logger;

    public ApimlLogger(Class<?> clazz, MessageService messageService) {
        this.messageService = messageService;
        this.logger = LoggerFactory.getLogger(clazz);
    }

    public static ApimlLogger of(Class<?> clazz,
                          MessageService messageService) {
        return new ApimlLogger(clazz, messageService);
    }

    public static ApimlLogger empty() {
        return new ApimlLogger(ApimlLogger.class, null);
    }

    public void log(String key, Object... parameters) {
        if (messageService != null) {
            Message message = messageService.createMessage(key, parameters);
            log(message);
        }
    }

    @SuppressWarnings("squid:S2629")
    private void log(Message message) {
        switch (message.getMessageTemplate().getType()) {
            case TRACE: logger.trace(message.mapToLogMessage()); break;
            case DEBUG: logger.debug(message.mapToLogMessage()); break;
            case INFO: logger.info(message.mapToLogMessage()); break;
            case WARNING: logger.warn(message.mapToLogMessage()); break;
            case ERROR: logger.error(message.mapToLogMessage()); break;
        }
    }
}
