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
import com.ca.mfaas.message.core.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class which allows to control log messages through {@link MessageService}.
 * <b>Example:</b>
 * {@code
 * ApimlLogger logger = ApimlLogger.of(SampleClass.cass, messageService)
 * }
 */
public final class ApimlLogger {

    private final MessageService messageService;
    private final Logger logger;

    public ApimlLogger(Class<?> clazz, MessageService messageService) {
        this.messageService = messageService;
        this.logger = LoggerFactory.getLogger(clazz);
    }

    /**
     * Method which allows to create an ApimlLogger object.
     *
     * @param clazz          the class for which is logger used
     * @param messageService used to produce the message
     * @return {@link ApimlLogger}
     */
    public static ApimlLogger of(Class<?> clazz,
                                 MessageService messageService) {
        return new ApimlLogger(clazz, messageService);
    }


    /**
     * Method which returns ApimlLogger with null {@link MessageService}.
     * It is used for unit test environment.
     *
     * @return {@link ApimlLogger}
     */
    public static ApimlLogger empty() {
        return new ApimlLogger(ApimlLogger.class, null);
    }

    /**
     * Method which allows to create a specific message with specific parameters and log it in its level type.
     *
     * @param key        of the message
     * @param parameters for message
     */
    public void log(String key, Object... parameters) {
        if (messageService != null) {
            Message message = messageService.createMessage(key, parameters);
            log(message.getMessageTemplate().getType(), message.mapToLogMessage());
        }
    }

    /**
     * Method which allows to log text in its level type.
     *
     * @param messageType  type of the message
     * @param text text for message
     * @param arguments arguments for message text
     * @throws IllegalArgumentException when parameters are null
     */
    @SuppressWarnings("squid:S2629")
    public void log(MessageType messageType, String text, Object... arguments) {
        if (messageType == null || text == null || arguments == null) {
            throw new IllegalArgumentException("Parameters can't be null");
        }

        switch (messageType) {
            case TRACE:
                logger.trace(text, arguments);
                break;
            case DEBUG:
                logger.debug(text, arguments);
                break;
            case INFO:
                logger.info(text, arguments);
                break;
            case WARNING:
                logger.warn(text, arguments);
                break;
            case ERROR:
                logger.error(text, arguments);
                break;
            default:
                logger.warn("The following message contains invalid message type.");
                logger.info(text, arguments);
                break;
        }
    }
}
