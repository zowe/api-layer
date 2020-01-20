/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.message.core;

import org.zowe.apiml.message.dummy.DummyMessageService;
import org.zowe.apiml.message.template.MessageTemplate;
import org.zowe.apiml.message.template.MessageTemplates;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class AbstractMessageServiceTest {

    private final AbstractMessageService abstractMessageService = new DummyMessageService("path");

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldCreateMessage() {
        Message message = abstractMessageService.createMessage("apiml.common.serviceTimeout", "3000");
        assertEquals("Message texts are different",
            "No response received within the allowed time: 3000", message.getConvertedText());
    }

    @Test
    public void shouldReturnInvalidMessage_IfKeyInvalid() {
        Message message = abstractMessageService.createMessage("invalidKey", "parameter");
        String expectedMessage = "Internal error: Invalid message key 'invalidKey' provided. No default message found. Please contact support of further assistance.";
        assertEquals("Message texts are different", expectedMessage, message.getConvertedText());
    }

    @Test
    public void shouldReturnInvalidMessageTextFormat_IfTheFormatIsIllegal() {
        Message message = abstractMessageService.createMessage("apiml.common.serviceTimeout.illegalFormat", "3000");
        String expectedMessageText = "Internal error: Invalid message text format. Please contact support for further assistance.";
        assertEquals("Message texts are different", expectedMessageText, message.getConvertedText());

        message = abstractMessageService.createMessage("apiml.common.serviceTimeout.missingFormat", "3000");
        assertEquals("Message texts are different", expectedMessageText, message.getConvertedText());
    }

    @Test
    public void shouldReturnInvalidMessageTextFormat_IfTheParamIsMissing() {
        Message message = abstractMessageService.createMessage("apiml.common.serviceTimeout.missingFormat", "3000");
        String expectedMessageText = "Internal error: Invalid message text format. Please contact support for further assistance.";
        assertEquals("Message texts are different", expectedMessageText, message.getConvertedText());
    }

    @Test
    public void shouldCreateMessages_IfMultipleParametersArePassed() {
        List<Object[]> parameters = new ArrayList<>();
        parameters.add(new Object[]{"2000"});
        parameters.add(new Object[]{"3000"});
        List<Message> messages = abstractMessageService.createMessage("apiml.common.serviceTimeout", parameters);
        assertEquals("Message texts are different", "No response received within the allowed time: 2000", messages.get(0).getConvertedText());
        assertEquals("Message texts are different", "No response received within the allowed time: 3000", messages.get(1).getConvertedText());
    }

    @Test
    public void shouldNotCreateMessages_IfEmptyParametersArePassed() {
        List<Object[]> parameters = new ArrayList<>();
        List<Message> messages = abstractMessageService.createMessage("apiml.common.serviceTimeout", parameters);
        assertEquals("Generated different number of messages than expected", 0, messages.size());
    }

    @Test
    public void shouldNotCreateMessages_IfEmptyParameterListIsPassed() {
        Message messages = abstractMessageService.createMessage("apiml.common.stringParamMessage", new ArrayList<String>());
        assertEquals("Unexpected message format for empty parameters list", "This message has one param: []", messages.getConvertedText());
    }

    @Test
    public void shouldThrowException_IfThereAreDuplicatedMessageNumbers() {
        MessageTemplate message = new MessageTemplate();
        message.setKey("apiml.common.serviceTimeout");
        message.setNumber("ZWEAM700");

        MessageTemplate message2 = new MessageTemplate();
        message2.setKey("apiml.common.serviceTimeout.illegalFormat");
        message2.setNumber("ZWEAM700");

        MessageTemplates messages = new MessageTemplates();
        messages.setMessages(
            Arrays.asList(message, message2)
        );

        exception.expect(DuplicateMessageException.class);
        exception.expectMessage("Message template with number [ZWEAM700] already exists");

        abstractMessageService.addMessageTemplates(messages);
    }

}
