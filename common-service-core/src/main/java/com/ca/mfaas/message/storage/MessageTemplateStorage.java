/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.message.storage;

import com.ca.mfaas.message.core.DuplicateMessageException;
import com.ca.mfaas.message.template.MessageTemplate;
import com.ca.mfaas.message.template.MessageTemplates;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Message template storage class
 */
public class MessageTemplateStorage {
    private final Map<String, MessageTemplate> messageTemplateMap = new HashMap<>();


    /**
     * Retrieve message template from the storage by message key
     *
     * @param key Message Key
     * @return message template
     */
    public Optional<MessageTemplate> getMessageTemplate(String key) {
        return Optional.ofNullable(messageTemplateMap.get(key));
    }


    /**
     * Method for adding message templates to storage
     *
     * @param messages Message templates
     */
    public void addMessageTemplates(MessageTemplates messages) {
        messages.getMessages().forEach(this::addMessageTemplateToStorage);
    }

    private void addMessageTemplateToStorage(MessageTemplate message) {
        if (!messageTemplateMap.containsKey(message.getKey())) {
            messageTemplateMap.put(message.getKey(), message);
        } else {
            String exceptionMessage = String.format("Message with key '%s' already exists", message.getKey());
            throw new DuplicateMessageException(exceptionMessage);
        }
    }
}
