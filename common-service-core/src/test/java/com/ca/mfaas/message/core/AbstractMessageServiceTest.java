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

import com.ca.mfaas.message.dummy.DummyMessageService;
import com.ca.mfaas.message.template.MessageTemplate;
import com.ca.mfaas.message.template.MessageTemplates;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IllegalFormatConversionException;
import java.util.List;

import static org.junit.Assert.*;

public class AbstractMessageServiceTest {

    AbstractMessageService abstractMessageService = new DummyMessageService("path");

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldCreateMessage() {
        Message message = abstractMessageService.createMessage("messageKey1", "p1");
        assertEquals("Test message with parameter p1", message.getConvertedText());
    }

    @Test
    public void shouldReturnInvalidMessageIfKeyInvalid() {
        Message message = abstractMessageService.createMessage("invalidKey", "parameter");
        String expectedMessage = "Internal error: Invalid message key 'invalidKey' provided. No default message found. Please contact CA support of further assistance.";
        assertEquals(expectedMessage, message.getConvertedText());
    }

    @Test
    public void shouldThrowExceptionIfTheFormatIsIllegal() {
        exception.expect(IllegalFormatConversionException.class);
        abstractMessageService.createMessage("com.ca.mfaas.common.invalidMessageTextFormat", new Object[]{ "3000" });
    }

    @Test
    public void shouldCreateMessagesWhenMultipleParametersArePassed() {
        List<Object[]> parameters = new ArrayList<Object[]>();
        parameters.add(new Object[]{"p1"});
        parameters.add(new Object[]{"p2"});
        List<Message> messages = abstractMessageService.createMessage("messageKey1", parameters);
        assertEquals("Test message with parameter p1", messages.get(0).getConvertedText());
        assertEquals("Test message with parameter p2", messages.get(1).getConvertedText());
    }

    @Test
    public void shouldThrowExceptionIfThereAreDuplicatedMessages() {
        MessageTemplate message = new MessageTemplate();
        message.setKey("messageKey1");
        message.setNumber("0001");
        message.setText("Test message 1");
        MessageTemplates messages = new MessageTemplates();
        messages.setMessages(
            Arrays.asList(message)
        );

        exception.expect(DuplicateMessageException.class);
        exception.expectMessage("Message with key 'messageKey1' already exists");

        abstractMessageService.addMessageTemplates(messages);
    }

}
