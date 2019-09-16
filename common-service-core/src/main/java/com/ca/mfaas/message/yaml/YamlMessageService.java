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

@Slf4j
public class YamlMessageService implements MessageService {

    private static final String COMMON_MESSAGES = "/mfs-common-messages.yml";

    private final MessageTemplateStorage messageTemplateStorage;

    public YamlMessageService() {
        messageTemplateStorage = new MessageTemplateStorage();
        loadMessages(COMMON_MESSAGES);
    }

    public YamlMessageService(String messagesFilePath) {
        this();
        loadMessages(messagesFilePath);
    }

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
