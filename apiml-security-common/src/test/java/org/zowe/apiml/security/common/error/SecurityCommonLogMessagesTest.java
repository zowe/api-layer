/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.error;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityCommonLogMessagesTest {

    private static final String MISSING_AUTHENTICATION_KEY = "org.zowe.apiml.zaas.security.schema.missingAuthentication";

    @Nested
    class MissingAuthenticationMessage {

        private final MessageService messageService = new YamlMessageService("/security-common-log-messages.yml");

        @Test
        void givenSharedBundle_whenMissingAuthenticationKey_thenMessageResolves() {
            Message message = messageService.createMessage(MISSING_AUTHENTICATION_KEY);

            assertEquals("ZWEAG160E No authentication provided in the request", message.mapToLogMessage());
        }
    }
}
