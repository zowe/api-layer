/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.message;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.template.MessageTemplate;
import org.zowe.apiml.message.yaml.YamlMessageService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CommonLogMessagesTest {

    private final MessageService messageService = new YamlMessageService("/common-log-messages.yml");

    @ParameterizedTest
    @ValueSource(strings = {
        "org.zowe.apiml.common.notFound",
        "org.zowe.apiml.common.methodNotAllowed",
        "org.zowe.apiml.common.unsupportedMediaType"
    })
    void errorMessagesShouldHaveReasonAndAction(String messageKey) {
        Message message = messageService.createMessage(messageKey);
        MessageTemplate template = message.getMessageTemplate();

        assertNotNull(template.getReason(), "Message " + messageKey + " is missing 'reason' field");
        assertFalse(template.getReason().isBlank(), "Message " + messageKey + " has blank 'reason' field");

        assertNotNull(template.getAction(), "Message " + messageKey + " is missing 'action' field");
        assertFalse(template.getAction().isBlank(), "Message " + messageKey + " has blank 'action' field");
    }
}
