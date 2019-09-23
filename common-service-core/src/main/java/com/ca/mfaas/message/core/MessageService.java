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


import java.util.List;

/**
 * Service for creating {@link Message} by string key and list of parameters.
 * See default implementation {@link com.ca.mfaas.message.yaml.YamlMessageService}.
 */
public interface MessageService {

    /**
     * Create {@link Message} that contains one {@link Message}
     * for provided key with array of parameters.
     *
     * @param key        of message in messages.yml file
     * @param parameters for message
     * @return {@link Message} for key
     */
    Message createMessage(String key, Object... parameters);

    /**
     * Create list {@link Message} that contains list of {@link Message}
     * with same key and provided parameters.
     *
     * @param key        of message in messages.yml file
     * @param parameters list that contains arrays of parameters
     * @return list {@link Message} for key
     */
    List<Message> createMessage(String key, List<Object[]> parameters);

    /**
     * Load messages to the context from the provided message file path
     *
     * @param messagesFilePath path of the message file
     */
    void loadMessages(String messagesFilePath);
}
