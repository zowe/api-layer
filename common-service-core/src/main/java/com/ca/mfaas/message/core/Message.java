/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.message.core;

import com.ca.mfaas.message.api.ApiMessage;
import com.ca.mfaas.message.api.ApiMessageView;
import com.ca.mfaas.message.template.MessageTemplate;
import org.apache.commons.lang.StringEscapeUtils;

import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

/**
 * Message creator immutable class
 */
public final class Message {

    public static final String INVALID_KEY_MESSAGE = "com.ca.mfaas.common.invalidMessageKey";
    public static final String INVALID_MESSAGE_TEXT_FORMAT = "com.ca.mfaas.common.invalidMessageTextFormat";

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
     * Return a {@link Message} object for the specified key after text and parameters validation.
     *
     * @param requestedKey the message key.
     * @param messageTemplate the messageTemplate.
     * @param messageParameters the object containing the message parameters.
     * @return {@link Message}
     */
    public static Message of(String requestedKey,
                             MessageTemplate messageTemplate,
                             Object[] messageParameters) {
        //validation
        Objects.requireNonNull(requestedKey, "requestedKey");
        Objects.requireNonNull(messageTemplate, "messageTemplate");

        validateMessageTextFormat(messageTemplate.getText(), messageParameters);
        messageParameters = validateParameters(messageTemplate.getKey(), requestedKey, messageParameters);

        return new Message(requestedKey, messageTemplate, messageParameters);
    }

    /**
     * Validate the message text and parameters returning them as formatted String.
     *
     * @param messageText the message text.
     * @param messageParameters the object containing the message parameters.
     * @return a formatted String
     */
    private static String validateMessageTextFormat(String messageText, Object[] messageParameters) {
        return String.format(messageText, messageParameters);
    }

    /**
     * Check if the key is equal to the invalid key message. If it is, the parameter is set to the requested key,
     * which will be displayed in the invalid key message text, otherwise the passed parameters are returned.
     * @param messageKey the message key.
     * @param requestedKey the message key.
     * @param parameters the object containing the message parameters.
     * @return an Object
     */
    private static Object[] validateParameters(String messageKey, String requestedKey, Object... parameters) {
        if (messageKey.equals(Message.INVALID_KEY_MESSAGE)) {
            return new Object[]{ requestedKey };
        } else {
            return parameters;
        }
    }

    /**
     * Converts the text after processing with {@link MessageTemplate} and Object[] message parameters by {@link Message} class.
     * @return escaped characters in a String using HTML entities
     */
    public String getConvertedText() {
        String convertedText = validateMessageTextFormat(messageTemplate.getText(), messageParameters);
        convertedText = StringEscapeUtils.escapeHtml(convertedText);
        return convertedText;
    }

    /**
     * @return a message in the format that can be printed to console as a single line or displayed to the user
     */
    public String mapToReadableText() {
        return String.format("%s%s %s {%s}",
            messageTemplate.getNumber(),
            messageTemplate.getType().toChar(),
            getConvertedText(),
            generateMessageInstanceId());
    }

    /**
     * Returns UI model as a list of API Message. It can be used for REST APIs error messages
     * @return {@link ApiMessageView}
     */
    public ApiMessageView mapToView() {
        return new ApiMessageView(Collections.singletonList(mapToApiMessage()));
    }

    /**
     * Returns UI model as a single {@link ApiMessage}.
     * @return {@link ApiMessage}
     */
    public ApiMessage mapToApiMessage() {
        return new ApiMessage(
            requestedKey,
            messageTemplate.getType(),
            messageTemplate.getNumber(),
            getConvertedText());
    }

    /**
     * Returns log message as a text.
     * @return a String
     */
    public String mapToLogMessage() {
        return mapToReadableText();
    }

    /**
     * @return a random unique ID for the log message
     */
    private String generateMessageInstanceId() {
        return UUID.randomUUID().toString();
    }

    /**
     * @return a message template
     */
    public MessageTemplate getMessageTemplate() {
        return messageTemplate;
    }
}
