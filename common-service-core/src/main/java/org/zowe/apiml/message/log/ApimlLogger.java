/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.message.log;

import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.util.ObjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

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
    private static final Marker marker = MarkerFactory.getMarker("APIML-LOGGER");

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
        ObjectUtil.requireNotNull(key, "key can't be null");
        ObjectUtil.requireNotNull(parameters, "parameters can't be null");

        if (messageService != null) {
            Message message = messageService.createMessage(key, parameters);
            log(message);
        }
    }

    /**
     * Method which allows to log text in its level type, without passing message parameters.
     *
     * @param message the message
     */
    public void log(Message message) {
        ObjectUtil.requireNotNull(message, "message can't be null");
        ObjectUtil.requireNotNull(message.getMessageTemplate(), "message template can't be null");

        log(message.getMessageTemplate().getType(), message.mapToLogMessage());
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
        ObjectUtil.requireNotNull(messageType, "messageType can't be null");
        ObjectUtil.requireNotNull(text, "text can't be null");
        ObjectUtil.requireNotNull(arguments, "arguments can't be null");

        switch (messageType) {
            case TRACE:
                logger.trace(marker, text, arguments);
                break;
            case DEBUG:
                logger.debug(marker, text, arguments);
                break;
            case INFO:
                logger.info(marker, text, arguments);
                break;
            case WARNING:
                logger.warn(marker, text, arguments);
                break;
            case ERROR:
                logger.error(marker, text, arguments);
                break;
            default:
                logger.warn(marker, "The following message contains invalid message type.");
                logger.info(marker, text, arguments);
                break;
        }
    }
}
