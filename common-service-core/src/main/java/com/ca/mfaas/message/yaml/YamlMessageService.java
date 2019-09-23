/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.message.yaml;

import com.ca.mfaas.message.core.Message;
import com.ca.mfaas.message.core.MessageLoadException;
import com.ca.mfaas.message.core.DuplicateMessageException;
import com.ca.mfaas.message.core.MessageService;
import com.ca.mfaas.message.core.MessageType;
import com.ca.mfaas.message.storage.MessageTemplateStorage;
import com.ca.mfaas.message.template.MessageTemplate;
import com.ca.mfaas.message.template.MessageTemplates;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.io.InputStream;
import java.util.IllegalFormatConversionException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link MessageService} that uses messages.yml as source for messages.
 */
@Slf4j
public class YamlMessageService implements MessageService {

    private static final String COMMON_MESSAGES = "/mfs-common-messages.yml";

    private final MessageTemplateStorage messageTemplateStorage;

    /**
     * Constructor that creates only common messages.
     */
    public YamlMessageService() {
        messageTemplateStorage = new MessageTemplateStorage();
        loadMessages(COMMON_MESSAGES);
    }

    /**
     * Constructor that creates common messages and messages from file.
     *
     * @param messagesFilePath path to file with messages.
     */
    public YamlMessageService(String messagesFilePath) {
        this();
        loadMessages(messagesFilePath);
    }

    /**
     * Load messages to the context from the provided message file path
     *
     * @param messagesFilePath path of the message file
     * @throws MessageLoadException      when a message couldn't loaded or has wrong definition
     * @throws DuplicateMessageException when a message is already defined before
     */
    @Override
    public void loadMessages(String messagesFilePath) {
        try (InputStream in = YamlMessageService.class.getResourceAsStream(messagesFilePath)) {
            Yaml yaml = new Yaml();
            MessageTemplates messageTemplates = yaml.loadAs(in, MessageTemplates.class);
            messageTemplateStorage.addMessageTemplates(messageTemplates);
        } catch (YAMLException | IOException e) {
            throw new MessageLoadException("There is problem with reading application messages file: " + messagesFilePath, e);
        }
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
        } catch (IllegalFormatConversionException exception) {
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

    private MessageTemplate validateMessageTemplate(String key) {
        return messageTemplateStorage.getMessageTemplate(key)
            .orElseGet(() -> {
                log.debug("Invalid message key '{}' was used. Please resolve this problem.", key);
                return messageTemplateStorage.getMessageTemplate(Message.INVALID_KEY_MESSAGE).orElseGet(
                    this::getInvalidMessageTemplate);
            });
    }

    public MessageTemplate getInvalidMessageTemplate() {
        String text = "Internal error: Invalid message key '%s' provided. No default message found. " +
            "Please contact CA support of further assistance.";
        return new MessageTemplate(Message.INVALID_KEY_MESSAGE, "MFS0001", MessageType.ERROR, text);
    }
}
