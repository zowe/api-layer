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
import com.ca.mfaas.message.core.MessageType;
import com.ca.mfaas.message.template.MessageTemplate;
import com.ca.mfaas.message.template.MessageTemplates;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class MessageTemplateStorageTest {
    private final MessageTemplateStorage messageTemplateStorage = new MessageTemplateStorage();

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    @Ignore
    public void testAddMessageTemplates() {
        MessageTemplates messageTemplates = new MessageTemplates(Collections.singletonList(
            new MessageTemplate("key", "number", MessageType.ERROR, "error message")
        ));

        messageTemplateStorage.addMessageTemplates(messageTemplates);

        Optional<MessageTemplate> optionalMessageTemplate = messageTemplateStorage.getMessageTemplate("key");
        assertTrue("Message template is null", optionalMessageTemplate.isPresent());
        assertEquals("Message template is not equal",
            messageTemplates.getMessages().get(0), optionalMessageTemplate.get());
    }


    @Test
    @Ignore
    public void testAddMessageTemplates_whenDuplicatedKeyMessagesArePresent() {
        exceptionRule.expect(DuplicateMessageException.class);
        exceptionRule.expectMessage("Message with key 'key' already exists");

        MessageTemplates messageTemplates = new MessageTemplates(Arrays.asList(
            new MessageTemplate("key", "number1", MessageType.ERROR, "error message"),
            new MessageTemplate("key", "number2", MessageType.ERROR, "error message")
        ));

        messageTemplateStorage.addMessageTemplates(messageTemplates);
    }

    @Test
    @Ignore
    public void testAddMessageTemplates_whenDuplicatedNumberMessagesArePresent() {
        exceptionRule.expect(DuplicateMessageException.class);
        exceptionRule.expectMessage("Message with key 'number' already exists");

        MessageTemplates messageTemplates = new MessageTemplates(Arrays.asList(
            new MessageTemplate("key1", "number", MessageType.ERROR, "error message"),
            new MessageTemplate("key2", "number", MessageType.ERROR, "error message")
        ));

        messageTemplateStorage.addMessageTemplates(messageTemplates);
    }
}
