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

import org.zowe.apiml.message.storage.MessageTemplateStorage;
import org.zowe.apiml.message.template.MessageTemplate;
import org.zowe.apiml.message.template.MessageTemplates;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Abstract class which implements the {@link MessageService} interface.
 * It creates messages from the file, validates and adds them to the {@link MessageTemplateStorage}.
 */
@Slf4j
public abstract class AbstractMessageService implements MessageService {

    private final MessageTemplateStorage messageTemplateStorage;


    /**
     * Constructor loads messages from a file.
     *
     * @param messagesFilePath path to the file with messages.
     */
    public AbstractMessageService(String messagesFilePath) {
        messageTemplateStorage = new MessageTemplateStorage();
        loadMessages(messagesFilePath);
    }

    /**
     * Creates {@link Message} with the key and list of parameters.
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

    /**
     * Validate and add a {@link MessageTemplates} to the {@link MessageTemplateStorage}.
     *
     * @param messageTemplates the list of message templates
     */
    protected final void addMessageTemplates(MessageTemplates messageTemplates) {
        validateMessageTemplates(messageTemplates);
        messageTemplateStorage.addMessageTemplates(messageTemplates);
    }

    /**
     * Validate {@link MessageTemplates} by checking {@link MessageTemplate} occurrences with the same key.
     * If {@link MessageTemplate} occur, an exception is thrown
     *
     * @param messageTemplates the list of message templates
     * @throws DuplicateMessageException when a message key already exists
     */
    private void validateMessageTemplates(MessageTemplates messageTemplates) {
        String existedMessageTemplates = messageTemplates.getMessages()
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

        if (!existedMessageTemplates.equals("")) {
            String exceptionMessage = String.format("Message template with number [%s] already exists", existedMessageTemplates);
            throw new DuplicateMessageException(exceptionMessage);
        }
    }

    /**
     * Validate {@link MessageTemplate} by checking if the message with the a specific key exists in the {@link MessageTemplateStorage}.
     * If it does not, the method returns an invalid key message.
     *
     * @param key the message key
     * @return {@link MessageTemplate}
     */
    private MessageTemplate validateMessageTemplate(String key) {
        return messageTemplateStorage.getMessageTemplate(key)
            .orElseGet(() -> {
                log.debug("Invalid message key '{}' was used. Please resolve this problem.", key);
                return messageTemplateStorage.getMessageTemplate(Message.INVALID_KEY_MESSAGE).orElseGet(
                    this::getInvalidMessageTemplate);
            });
    }

    /**
     * Returns the invalid key message {@link MessageTemplate}.
     *
     * @return {@link MessageTemplate}
     */
    private MessageTemplate getInvalidMessageTemplate() {
        String text = "Internal error: Invalid message key '%s' provided. No default message found. " +
            "Please contact support of further assistance.";
        return new MessageTemplate(Message.INVALID_KEY_MESSAGE, "ZWEAM102", MessageType.ERROR, text);
    }
}
