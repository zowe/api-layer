/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.error.impl;

import com.ca.mfaas.error.DuplicateMessageException;

import java.util.HashMap;
import java.util.Map;

public class ErrorMessageStorage {
    private final Map<String, ErrorMessage> keyMap;
    private final Map<String, ErrorMessage> numberMap;

    public ErrorMessageStorage() {
        this.keyMap = new HashMap<>();
        this.numberMap = new HashMap<>();
    }

    public ErrorMessage getErrorMessage(String key) {
        return keyMap.get(key);
    }

    public void addMessages(ErrorMessages messages) {
        for (ErrorMessage message : messages.getMessages()) {
            if (!keyMap.containsKey(message.getKey())) {
                if (!numberMap.containsKey(message.getNumber())) {
                    keyMap.put(message.getKey(), message);
                    numberMap.put(message.getNumber(), message);
                } else {
                    String exceptionMessage = String.format("Message with number '%s' is already exists", message.getNumber());
                    throw new DuplicateMessageException(exceptionMessage);
                }
            } else {
                String exceptionMessage = String.format("Message with key '%s' is already exists", message.getKey());
                throw new DuplicateMessageException(exceptionMessage);
            }
        }
    }
}
