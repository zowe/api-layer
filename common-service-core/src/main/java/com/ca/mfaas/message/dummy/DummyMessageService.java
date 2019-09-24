/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.message.dummy;

import com.ca.mfaas.message.core.AbstractMessageService;
import com.ca.mfaas.message.template.MessageTemplate;
import com.ca.mfaas.message.template.MessageTemplates;

import java.util.Arrays;

public class DummyMessageService extends AbstractMessageService {

    public DummyMessageService(String messagesFilePath) {
        super(messagesFilePath);
    }

    @Override
    public void loadMessages(String messagesFilePath) {
        MessageTemplates messageTemplates = new MessageTemplates();
        MessageTemplate message = new MessageTemplate();
        MessageTemplate message2 = new MessageTemplate();
        MessageTemplate message3 = new MessageTemplate();

        message.setKey("messageKey1");
        message.setNumber("0001");
        message.setText("Test message with parameter %s");

        message2.setKey("com.ca.mfaas.common.invalidMessageTextFormat");
        message2.setNumber("0002");
        message2.setText("Test message - expects decimal number %d");

        message3.setKey("messageKey3");
        message3.setNumber("0003");
        message3.setText("Test Message with parameters %s and %s");

        messageTemplates.setMessages(
            Arrays.asList(message, message2, message3)
        );

        super.addMessageTemplates(messageTemplates);
    }
}
