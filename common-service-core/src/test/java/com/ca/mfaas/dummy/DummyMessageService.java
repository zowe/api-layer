/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.dummy;

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
        message.setKey("apiml.common.serviceTimeout");
        message.setNumber("MFS0104");
        message.setText("No response received within the allowed time: %s");

        MessageTemplate message2 = new MessageTemplate();
        message2.setKey("apiml.common.serviceTimeout.illegalFormat");
        message2.setNumber("MFS0104F");
        message2.setText("No response received within the allowed time: %d");

        MessageTemplate message3 = new MessageTemplate();
        message3.setKey("apiml.common.serviceTimeout.missingFormat");
        message3.setNumber("MFS0104M");
        message3.setText("No response received within the allowed time: %s %s");

        MessageTemplate message4 = new MessageTemplate();
        message4.setKey("com.ca.mfaas.common.invalidMessageTextFormat");
        message4.setNumber("MFS0002");
        message4.setText("Internal error: Invalid message text format. Please contact support for further assistance.");

        messageTemplates.setMessages(
            Arrays.asList(message, message2, message3, message4)
        );

        super.addMessageTemplates(messageTemplates);
    }
}
