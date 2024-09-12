/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.message.core;

import org.zowe.apiml.message.api.ApiMessage;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.template.MessageTemplate;
import org.zowe.apiml.util.ObjectUtil;
import org.apache.commons.text.StringEscapeUtils;

import java.util.Collections;
import java.util.IllegalFormatConversionException;
import java.util.MissingFormatArgumentException;

/**
 * Message creator immutable class
 */
public final class Message {
    public static final String INVALID_KEY_MESSAGE = "org.zowe.apiml.common.invalidMessageKey";
    public static final String INVALID_KEY_MESSAGE_TEXT = "Internal error: Invalid message key '%s' provided. " +
        "No default message found. Please contact support of further assistance.";
    public static final String INVALID_MESSAGE_TEXT_FORMAT = "org.zowe.apiml.common.invalidMessageTextFormat";

    private final String requestedKey;
    private final MessageTemplate messageTemplate;
    private final Object[] messageParameters;


    private Message(String requestedKey,
                    MessageTemplate messageTemplate,
                    Object[] messageParameters) {
        this.requestedKey = requestedKey;
        this.messageTemplate = messageTemplate;
        this.messageParameters = messageParameters;
    }

    /**
     * Returns a {@link Message} object for the specified key after text and parameters validation.
     *
     * @param requestedKey      the message key.
     * @param messageTemplate   the messageTemplate.
     * @param messageParameters the object containing the message parameters.
     * @return {@link Message}
     */
    public static Message of(String requestedKey,
                             MessageTemplate messageTemplate,
                             Object[] messageParameters) {

        ObjectUtil.requireNotNull(requestedKey, "requestedKey can't be null");
        ObjectUtil.requireNotNull(messageTemplate, "messageTemplate can't be null");
        ObjectUtil.requireNotNull(messageParameters, "messageParameters can't be null");

        messageParameters = validateParameters(messageTemplate.getKey(), requestedKey, messageParameters);
        validateMessageTextFormat(messageTemplate.getText(), messageParameters);

        return new Message(requestedKey, messageTemplate, messageParameters);
    }

    /**
     * Returns an {@link MessageType#ERROR} {@link Message} object indicating that the specified key is invalid.
     * The {@link MessageTemplate} for this message has key {@link #INVALID_KEY_MESSAGE}.
     *
     * @param requestedKey the invalid key.
     * @return {@link Message}
     */
    public static Message invalidKeyMessage(String requestedKey) {
        return new Message(requestedKey, new MessageTemplate(Message.INVALID_KEY_MESSAGE, "ZWEAM102",
            MessageType.ERROR, Message.INVALID_KEY_MESSAGE_TEXT), new Object[]{requestedKey});
    }

    /**
     * Validate the message text and parameters returning them as formatted String.
     *
     * @param messageText       the message text.
     * @param messageParameters the object containing the message parameters.
     * @return a formatted String
     * @throws MissingFormatArgumentException   when the amount of parameters is less than required.
     * @throws IllegalFormatConversionException when format is not valid.
     */
    private static String validateMessageTextFormat(String messageText, Object[] messageParameters) {
        return String.format(messageText, messageParameters);
    }

    /**
     * Checks whether messageKey matches Message.INVALID_KEY_MESSAGE
     *
     * @param messageKey   the message key.
     * @param requestedKey the message key.
     * @param parameters   the object containing the message parameters.
     * @return the requested key as an array if it  matches Message.INVALID_KEY_MESSAGE. Otherwise, it returns parameters
     */
    private static Object[] validateParameters(String messageKey, String requestedKey, Object... parameters) {
        if (messageKey.equals(Message.INVALID_KEY_MESSAGE)) {
            return new Object[]{requestedKey};
        } else {
            return parameters;
        }
    }

    /**
     * Converts the text with {@link MessageTemplate} and Object[] message parameters.
     *
     * @return escaped characters in the converted text using HTML entities
     */
    public String getConvertedText() {
        String convertedText = validateMessageTextFormat(messageTemplate.getText(), messageParameters);
        convertedText = StringEscapeUtils.escapeHtml4(convertedText);
        return convertedText;
    }

    /**
     * @return a message in the format that can be printed to console as a single line or displayed to the user
     */
    public String mapToReadableText() {
        return String.format("%s%s %s",
            messageTemplate.getNumber(),
            messageTemplate.getType().toChar(),
            getConvertedText());
    }

    /**
     * Returns UI model as a list of API Message. It can be used for REST APIs error messages
     *
     * @return {@link ApiMessageView}
     */
    public ApiMessageView mapToView() {
        return new ApiMessageView(Collections.singletonList(mapToApiMessage()));
    }

    /**
     * Returns UI model as a single {@link ApiMessage}.
     *
     * @return {@link ApiMessage}
     */
    public ApiMessage mapToApiMessage() {
        return new ApiMessage(
            requestedKey,
            messageTemplate.getType(),
            messageTemplate.getNumber() + messageTemplate.getType().toChar(),
            getConvertedText(),
            messageTemplate.getAction(),
            messageTemplate.getReason());
    }

    /**
     * Returns log message as a text.
     *
     * @return a String
     */
    public String mapToLogMessage() {
        return mapToReadableText();
    }

    /**
     * @return a message template
     */
    public MessageTemplate getMessageTemplate() {
        return messageTemplate;
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "[" +
            "requestedKey=" + requestedKey + ", " +
            "messageTemplate=" + messageTemplate +
            "]";
    }
}
