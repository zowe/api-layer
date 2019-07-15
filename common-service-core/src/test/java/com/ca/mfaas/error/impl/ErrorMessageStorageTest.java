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
import com.ca.mfaas.rest.response.MessageType;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class ErrorMessageStorageTest {
    private final ErrorMessageStorage errorMessageStorage = new ErrorMessageStorage();

    @Test
    public void getKeyTest() {
        ErrorMessages messages = new ErrorMessages(Collections.singletonList(
            new ErrorMessage("key", "number", MessageType.ERROR, "error message")
        ));

        errorMessageStorage.addMessages(messages);
        ErrorMessage notExistingKeyMessage = errorMessageStorage.getErrorMessage("some key");
        ErrorMessage existingKeyMessage = errorMessageStorage.getErrorMessage("key");

        assertNull(notExistingKeyMessage);
        assertEquals("key", existingKeyMessage.getKey());
        assertEquals("number", existingKeyMessage.getNumber());
        assertEquals("error message", existingKeyMessage.getText());
    }

    @Test(expected = DuplicateMessageException.class)
    public void addDuplicatedKeyMessages() {
        ErrorMessages messages = new ErrorMessages(Arrays.asList(
            new ErrorMessage("key", "number1", MessageType.ERROR, "error message"),
            new ErrorMessage("key", "number2", MessageType.ERROR, "error message")
        ));

        errorMessageStorage.addMessages(messages);
    }

    @Test(expected = DuplicateMessageException.class)
    public void addDuplicatedNumberMessages() {
        ErrorMessages messages = new ErrorMessages(Arrays.asList(
            new ErrorMessage("key1", "number", MessageType.ERROR, "error message"),
            new ErrorMessage("key2", "number", MessageType.ERROR, "error message")
        ));

        errorMessageStorage.addMessages(messages);
    }

}
