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

import org.zowe.apiml.message.api.ApiMessage;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.template.MessageTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.IllegalFormatConversionException;
import java.util.MissingFormatArgumentException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageTest {

    private Message message;

    @BeforeEach
    void init() {
        message = Message.of("org.zowe.apiml.common.serviceTimeout",
            createMessageTemplate("No response received within the allowed time: %s"),
            new Object[]{"3000"});
    }

    @Test
    void testRequestedKeyParam_whenItIsNull() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            Message.of(null, null, null);
        });
        assertEquals("requestedKey can't be null", exception.getMessage());
    }

    @Test
    void testMessageTemplateParam_whenItIsNull() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            Message.of("key", null, null);
        });
        assertEquals("messageTemplate can't be null", exception.getMessage());
    }

    @Test
    void testMessageParameters_whenItIsNull() {
        MessageTemplate mt = new MessageTemplate();
        Exception exception = assertThrows(IllegalArgumentException.class, () -> Message.of("key", mt, null));
        assertEquals("messageParameters can't be null", exception.getMessage());
    }

    @Test
    void testCheckConvertValidation_whenThereAreMoreParamThanRequested() {
        MessageTemplate mt = createMessageTemplate("No response received within the allowed time: %s %s");
        assertThrows(MissingFormatArgumentException.class, () -> {
            Message.of("org.zowe.apiml.common.serviceTimeout", mt, new Object[]{"3000"});
        });
    }


    @Test
    void testCheckConvertValidation_whenThereIsWrongFormatThanRequested() {
        MessageTemplate mt = createMessageTemplate("No response received within the allowed time: %d");
        assertThrows(IllegalFormatConversionException.class, () -> {
            Message.of("org.zowe.apiml.common.serviceTimeout", mt, new Object[]{"3000"});
        });
    }


    @Test
    void testGetConvertedText() {
        String actualConvertedText = message.getConvertedText();
        String expectedConvertedText = "No response received within the allowed time: 3000";
        assertEquals(expectedConvertedText, actualConvertedText, "Converted text is different");
    }

    @Test
    void testGetConvertedText_whenTextContainsHTMLEntities() {
        message = Message.of("org.zowe.apiml.common.serviceTimeout",
            createMessageTemplate("No response  <b>received</b> within the allowed time: %s"),
            new Object[]{"3000"});

        String actualConvertedText = message.getConvertedText();
        String expectedConvertedText = "No response  &lt;b&gt;received&lt;/b&gt; within the allowed time: 3000";
        assertEquals(expectedConvertedText, actualConvertedText, "Converted text is different");
    }

    @Test
    void testMapToReadableText() {
        String actualReadableText = message.mapToReadableText();
        String expectedReadableText = "No response received within the allowed time: 3000";
        assertTrue(actualReadableText.contains(expectedReadableText), "Readable text is different");
    }


    @Test
    void testMapToView() {
        ApiMessageView actualApiMessageView = message.mapToView();
        assertEquals(1, actualApiMessageView.getMessages().size(), "ApiMessageView doesn't contain single message");

        String expectedReadableText = "No response received within the allowed time: 3000";
        ApiMessageView expectedApiMessageView = new ApiMessageView(Collections.singletonList(
            new ApiMessage("org.zowe.apiml.common.serviceTimeout", MessageType.ERROR, "ZWEAM700E", expectedReadableText, null, null)));

        assertEquals(expectedApiMessageView, actualApiMessageView, "ApiMessageView is different");
    }


    @Test
    void testMapToApiMessage() {
        ApiMessage actualApiMessage = message.mapToApiMessage();

        String expectedReadableText = "No response received within the allowed time: 3000";
        ApiMessage expectedApiMessage = new ApiMessage("org.zowe.apiml.common.serviceTimeout", MessageType.ERROR, "ZWEAM700E", expectedReadableText, null, null);

        assertEquals(expectedApiMessage, actualApiMessage, "ApiMessage is different");
    }


    @Test
    void testMapToLogMessage() {
        String actualLogMessage = message.mapToLogMessage();
        String expectedLogMessage = "No response received within the allowed time: 3000";
        assertTrue(actualLogMessage.contains(expectedLogMessage), "Log Message is different");
    }


    private MessageTemplate createMessageTemplate(String messageText) {
        MessageTemplate messageTemplate = new MessageTemplate();
        messageTemplate.setKey("org.zowe.apiml.common.serviceTimeout");
        messageTemplate.setNumber("ZWEAM700");
        messageTemplate.setType(MessageType.ERROR);
        messageTemplate.setText(messageText);

        return messageTemplate;
    }

}
