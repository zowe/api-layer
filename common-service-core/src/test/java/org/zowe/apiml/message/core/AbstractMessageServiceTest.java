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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbstractMessageServiceTest {

    private final AbstractMessageService abstractMessageService = new DummyMessageService("path");

    @Test
    void shouldCreateMessage() {
        Message message = abstractMessageService.createMessage("org.zowe.apiml.common.serviceTimeout", "3000");
        assertEquals("No response received within the allowed time: 3000", message.getConvertedText(), "Message texts are different");
    }

    @Test
    void shouldReturnInvalidMessage_IfKeyInvalid() {
        Message message = abstractMessageService.createMessage("invalidKey", "parameter");
        String expectedMessage = "Internal error: Invalid message key 'invalidKey' provided. No default message found. Please contact support of further assistance.";
        assertEquals(expectedMessage, message.getConvertedText(), "Message texts are different");
    }

    @Test
    void shouldReturnInvalidMessageTextFormat_IfTheFormatIsIllegal() {
        Message message = abstractMessageService.createMessage("org.zowe.apiml.common.serviceTimeout.illegalFormat", "3000");
        String expectedMessageText = "Internal error: Invalid message text format. Please contact support for further assistance.";
        assertEquals(expectedMessageText, message.getConvertedText(), "Message texts are different");

        message = abstractMessageService.createMessage("org.zowe.apiml.common.serviceTimeout.missingFormat", "3000");
        assertEquals(expectedMessageText, message.getConvertedText(), "Message texts are different");
    }

    @Test
    void shouldReturnInvalidMessageTextFormat_IfTheParamIsMissing() {
        Message message = abstractMessageService.createMessage("org.zowe.apiml.common.serviceTimeout.missingFormat", "3000");
        String expectedMessageText = "Internal error: Invalid message text format. Please contact support for further assistance.";
        assertEquals(expectedMessageText, message.getConvertedText(), "Message texts are different");
    }

    @Test
    void shouldCreateMessages_IfMultipleParametersArePassed() {
        List<Object[]> parameters = new ArrayList<>();
        parameters.add(new Object[]{"2000"});
        parameters.add(new Object[]{"3000"});
        List<Message> messages = abstractMessageService.createMessage("org.zowe.apiml.common.serviceTimeout", parameters);
        assertEquals("No response received within the allowed time: 2000", messages.get(0).getConvertedText(), "Message texts are different");
        assertEquals("No response received within the allowed time: 3000", messages.get(1).getConvertedText(), "Message texts are different");
    }

    @Test
    void shouldNotCreateMessages_IfEmptyParametersArePassed() {
        List<Object[]> parameters = new ArrayList<>();
        List<Message> messages = abstractMessageService.createMessage("org.zowe.apiml.common.serviceTimeout", parameters);
        assertEquals(0, messages.size(), "Generated different number of messages than expected");
    }

    @Test
    void shouldNotCreateMessages_IfEmptyParameterListIsPassed() {
        Message messages = abstractMessageService.createMessage("org.zowe.apiml.common.stringParamMessage", new ArrayList<String>());
        assertEquals("This message has one param: []", messages.getConvertedText(), "Unexpected message format for empty parameters list");
    }

    @Test
    void shouldThrowException_IfThereAreDuplicatedMessageNumbers() {
        MessageTemplate message = new MessageTemplate();
        message.setKey("org.zowe.apiml.common.serviceTimeout");
        message.setNumber("ZWEAM700");

        MessageTemplate message2 = new MessageTemplate();
        message2.setKey("org.zowe.apiml.common.serviceTimeout.illegalFormat");
        message2.setNumber("ZWEAM700");

        MessageTemplates messages = new MessageTemplates();
        messages.setMessages(
            Arrays.asList(message, message2)
        );

        Exception exception = assertThrows(DuplicateMessageException.class, () -> {
            abstractMessageService.addMessageTemplates(messages);
        });
        assertEquals("Message template with number [ZWEAM700] already exists", exception.getMessage());
    }

}
