/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.message.storage;

import org.zowe.apiml.message.template.MessageTemplate;
import org.zowe.apiml.message.template.MessageTemplates;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Message template storage class
 */
public class MessageTemplateStorage {
    private final Map<String, MessageTemplate> messageTemplateMap = new HashMap<>();


    /**
     * Retrieves message template from the storage using 'key' parameter
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
        messageTemplateMap.put(message.getKey(), message);
    }
}
