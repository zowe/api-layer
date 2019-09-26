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

import com.ca.mfaas.message.api.ApiMessage;
import com.ca.mfaas.message.api.ApiMessageView;
import com.ca.mfaas.message.template.MessageTemplate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collections;
import java.util.IllegalFormatConversionException;
import java.util.MissingFormatArgumentException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MessageTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private Message message;

    @Before
    public void init() {
        message = Message.of("apiml.common.serviceTimeout",
            createMessageTemplate("No response received within the allowed time: %s"),
            new Object[]{"3000"});
    }

    @Test
    public void testRequestedKeyParam_whenItIsNull() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("requestedKey can't be null");

        Message.of(null, null, null);
    }

    @Test
    public void testMessageTemplateParam_whenItIsNull() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("messageTemplate can't be null");

        Message.of("key", null, null);
    }

    @Test
    public void testMessageParameters_whenItIsNull() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("messageParameters can't be null");

        Message.of("key", new MessageTemplate(), null);
    }

    @Test(expected = MissingFormatArgumentException.class)
    public void testCheckConvertValidation_whenThereAreMoreParamThanRequested() {
        Message.of("apiml.common.serviceTimeout",
            createMessageTemplate("No response received within the allowed time: %s %s"),
            new Object[]{"3000"});
    }


    @Test(expected = IllegalFormatConversionException.class)
    public void testCheckConvertValidation_whenThereIsWrongFormatThanRequested() {
        Message.of("apiml.common.serviceTimeout",
            createMessageTemplate("No response received within the allowed time: %d"),
            new Object[]{"3000"});
    }


    @Test
    public void testGetConvertedText() {
        String actualConvertedText = message.getConvertedText();
        String expectedConvertedText = "No response received within the allowed time: 3000";
        assertEquals("Converted text is different", expectedConvertedText, actualConvertedText);
    }

    @Test
    public void testGetConvertedText_whenTextContainsHTMLEntities() {
        message = Message.of("apiml.common.serviceTimeout",
            createMessageTemplate("No response  <b>received</b> within the allowed time: %s"),
            new Object[]{"3000"});

        String actualConvertedText = message.getConvertedText();
        String expectedConvertedText = "No response  &lt;b&gt;received&lt;/b&gt; within the allowed time: 3000";
        assertEquals("Converted text is different", expectedConvertedText, actualConvertedText);
    }

    @Test
    public void testMapToReadableText() {
        String actualReadableText = message.mapToReadableText();
        String expectedReadableText = "No response received within the allowed time: 3000";
        assertTrue("Readable text is different", actualReadableText.contains(expectedReadableText));
    }


    @Test
    public void testMapToView() {
        ApiMessageView actualApiMessageView = message.mapToView();
        assertEquals("ApiMessageView doesn't contain single message", 1, actualApiMessageView.getMessages().size());

        String expectedReadableText = "No response received within the allowed time: 3000";
        ApiMessageView expectedApiMessageView = new ApiMessageView(Collections.singletonList(
            new ApiMessage("apiml.common.serviceTimeout", MessageType.ERROR, "MFS0104", expectedReadableText)
        ));

        assertEquals("ApiMessageView is different", expectedApiMessageView, actualApiMessageView);
    }


    @Test
    public void testMapToApiMessage() {
        ApiMessage actualApiMessage = message.mapToApiMessage();

        String expectedReadableText = "No response received within the allowed time: 3000";
        ApiMessage expectedApiMessage = new ApiMessage("apiml.common.serviceTimeout", MessageType.ERROR, "MFS0104", expectedReadableText);

        assertEquals("ApiMessage is different", expectedApiMessage, actualApiMessage);
    }


    @Test
    public void testMapToLogMessage() {
        String actualLogMessage = message.mapToLogMessage();
        String expectedLogMessage = "No response received within the allowed time: 3000";
        assertTrue("Log Message is different", actualLogMessage.contains(expectedLogMessage));
    }


    private MessageTemplate createMessageTemplate(String messageText) {
        MessageTemplate messageTemplate = new MessageTemplate();
        messageTemplate.setKey("apiml.common.serviceTimeout");
        messageTemplate.setNumber("MFS0104");
        messageTemplate.setType(MessageType.ERROR);
        messageTemplate.setText(messageText);

        return messageTemplate;
    }

}
