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

import com.ca.mfaas.message.core.*;
import com.ca.mfaas.message.template.MessageTemplates;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Default implementation of {@link MessageService} that uses messages.yml as source for messages.
 */
public class YamlMessageService extends AbstractMessageService {

    private static final String COMMON_MESSAGES = "/mfs-common-messages.yml";

    /**
     * Constructor that creates only common messages.
     */
    public YamlMessageService() {
        super(COMMON_MESSAGES);
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
     * @throws MessageLoadException      when a message couldn't load or has a wrong definition
     * @throws DuplicateMessageException when a message is already defined
     */
    @Override
    public void loadMessages(String messagesFilePath) {
        try (InputStream in = YamlMessageService.class.getResourceAsStream(messagesFilePath)) {
            Yaml yaml = new Yaml();
            MessageTemplates messageTemplates = yaml.loadAs(in, MessageTemplates.class);
            super.addMessageTemplates(messageTemplates);
        } catch (YAMLException | IOException e) {
            throw new MessageLoadException("There is problem with reading application messages file: " + messagesFilePath, e);
        }
    }
}
