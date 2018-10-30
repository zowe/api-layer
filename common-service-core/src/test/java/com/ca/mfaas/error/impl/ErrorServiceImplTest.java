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

import com.ca.mfaas.error.ErrorService;
import com.ca.mfaas.rest.response.ApiMessage;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ErrorServiceImplTest {
    private final ErrorService errorService = new ErrorServiceImpl();

    @Test
    public void invalidMessageKey() {
        ApiMessage message = errorService.createApiMessage("nonExistingKey", "someParameter1", "someParameter2");

        assertEquals("MFS0001", message.getMessages().get(0).getMessageNumber());
        assertEquals("Internal error: Invalid message key 'nonExistingKey' is provided. Please contact support for further assistance.",
            message.getMessages().get(0).getMessageContent());
    }

    @Test
    public void validMessageKey() {
        ApiMessage message = errorService.createApiMessage("com.ca.mfaas.common.mediaTypeNotSupported");

        assertEquals("MFS0102", message.getMessages().get(0).getMessageNumber());
        assertEquals("Unsupported Media Type", message.getMessages().get(0).getMessageContent());
    }

    @Test
    public void constructorWithExistingFile() {
        ErrorService errorServiceFromFile = new ErrorServiceImpl("/test-messages.yml");

        ApiMessage message = errorServiceFromFile.createApiMessage("com.ca.mfaas.test.noArguments");

        assertEquals("CSC0001", message.getMessages().get(0).getMessageNumber());
        assertEquals("No arguments message", message.getMessages().get(0).getMessageContent());
    }

    @Test(expected = RuntimeException.class)
    public void constructorWithNotExistingFile() {
        ErrorService errorServiceFromFile = new ErrorServiceImpl("/some-not-existing-messages.yml");
        errorServiceFromFile.createApiMessage("com.ca.mfaas.test.noArguments");
    }

    @Test
    public void createListOfMessagesWithSameKey() {
        Object[] parametersEntity1 = new Object[]{null};
        Object[] parametersEntity2 = new Object[]{null};
        List<Object[]> parameters = new ArrayList<>();
        parameters.add(parametersEntity1);
        parameters.add(parametersEntity2);
        ApiMessage message = errorService.createApiMessage("com.ca.mfaas.common.invalidEndpointName", parameters);

        assertEquals("MFS0101", message.getMessages().get(0).getMessageNumber());
        assertEquals("Invalid endpoint", message.getMessages().get(0).getMessageContent());
    }

    @Test
    public void invalidMessageTextFormat() {
        ErrorService errorServiceFromFile = new ErrorServiceImpl("/test-messages.yml");
        ApiMessage message = errorServiceFromFile.createApiMessage("com.ca.mfaas.test.invalidParameterFormat",
            "test", "someParameter2");

        assertEquals("MFS0002", message.getMessages().get(0).getMessageNumber());
        assertEquals("Internal error: Invalid message text format. Please contact support for further assistance.",
            message.getMessages().get(0).getMessageContent());
    }

    @Test(expected = RuntimeException.class)
    public void constructorWithDuplicatedMessages() {
        ErrorService errorServiceFromFile = new ErrorServiceImpl("/test-duplicate-messages.yml");
        errorServiceFromFile.createApiMessage("com.ca.mfaas.test.noArguments");
    }
}
