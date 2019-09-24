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

import com.ca.mfaas.message.storage.MessageTemplateStorage;
import com.ca.mfaas.message.template.MessageTemplate;
import com.ca.mfaas.message.template.MessageTemplates;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractMessageService implements MessageService {

    private final MessageTemplateStorage messageTemplateStorage;


    /**
     * Constructor that creates common messages and messages from file.
     *
     * @param messagesFilePath path to file with messages.
     */
    public AbstractMessageService(String messagesFilePath) {
        messageTemplateStorage = new MessageTemplateStorage();
        loadMessages(messagesFilePath);
    }

    /**
     * Creates {@link Message} with key and list of parameters.
     *
     * @param key        of message in messages.yml file
     * @param parameters for message
     * @return {@link Message}
     */
    @Override
    public Message createMessage(String key, Object... parameters) {
        MessageTemplate messageTemplate = validateMessageTemplate(key);

        try {
            return Message.of(key, messageTemplate, parameters);
        } catch (IllegalFormatConversionException | MissingFormatArgumentException exception) {
            log.debug("Internal error: Invalid message format was used", exception);
            messageTemplate = validateMessageTemplate(Message.INVALID_MESSAGE_TEXT_FORMAT);
            return Message.of(key, messageTemplate, parameters);
        }
    }

    /**
     * Creates list {@link Message} with list of {@link Message}.
     *
     * @param key        of message in messages.yml file
     * @param parameters list that contains arrays of parameters
     * @return {@link Message}
     */
    @Override
    public List<Message> createMessage(String key, List<Object[]> parameters) {
        return parameters.stream()
            .filter(Objects::nonNull)
            .map(ob -> createMessage(key, ob))
            .collect(Collectors.toList());
    }

    public final void addMessageTemplates(MessageTemplates messageTemplates) {
        validateMessageTemplates(messageTemplates);
        messageTemplateStorage.addMessageTemplates(messageTemplates);
    }

    private void validateMessageTemplates(MessageTemplates messageTemplates) {
        String existedMesageTemplates = messageTemplates.getMessages()
            .stream()
            .collect(
                Collectors.groupingBy(
                    MessageTemplate::getNumber,
                    Collectors.counting()
                )
            )
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue() > 1)
            .map(Map.Entry::getKey)
            .collect(Collectors.joining(","));

        if (!existedMesageTemplates.equals("")) {
            String exceptionMessage = String.format("Message template with number [%s] already exists", existedMesageTemplates);
            throw new DuplicateMessageException(exceptionMessage);
        }
    }

    private MessageTemplate validateMessageTemplate(String key) {
        return messageTemplateStorage.getMessageTemplate(key)
            .orElseGet(() -> {
                log.debug("Invalid message key '{}' was used. Please resolve this problem.", key);
                return messageTemplateStorage.getMessageTemplate(Message.INVALID_KEY_MESSAGE).orElseGet(
                    this::getInvalidMessageTemplate);
            });
    }

    private MessageTemplate getInvalidMessageTemplate() {
        String text = "Internal error: Invalid message key '%s' provided. No default message found. " +
            "Please contact CA support of further assistance.";
        return new MessageTemplate(Message.INVALID_KEY_MESSAGE, "MFS0001", MessageType.ERROR, text);
    }
}
