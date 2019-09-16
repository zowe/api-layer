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

    public static Message of(String requestedKey,
                             MessageTemplate messageTemplate,
                             Object[] messageParameters) {
        //validation
        Objects.requireNonNull(requestedKey, "requestedKey");
        Objects.requireNonNull(messageTemplate, "messageTemplate");

        checkConvertValidation(messageTemplate, messageParameters);
        messageParameters = validateParameters(messageTemplate.getKey(), requestedKey, messageParameters);

        return new Message(requestedKey, messageTemplate, messageParameters);
    }

    private static void checkConvertValidation(MessageTemplate messageTemplate, Object[] messageParameters) {
        String.format(messageTemplate.getText(), messageParameters);
    }

    private static Object[] validateParameters(String messageKey, String requestedKey, Object... parameters) {
        if (messageKey.equals(Message.INVALID_KEY_MESSAGE)) {
            return new Object[]{ requestedKey };
        } else {
            return parameters;
        }
    }

    public String getConvertedText() {
        String convertedText = String.format(messageTemplate.getText(), messageParameters);
        convertedText = StringEscapeUtils.escapeHtml(convertedText);
        return convertedText;
    }

    public String mapToReadableText() {
        //check is not null
        return messageTemplate.getNumber() + messageTemplate.getType().toChar() + ' ' + getConvertedText();
    }

    public ApiMessageView mapToView() {
        ApiMessage apiMessage = new ApiMessage(
            requestedKey,
            messageTemplate.getType(),
            messageTemplate.getNumber(),
            getConvertedText());
        return new ApiMessageView(Collections.singletonList(apiMessage));
    }
}
