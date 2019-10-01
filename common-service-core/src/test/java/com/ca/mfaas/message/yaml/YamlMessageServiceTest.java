/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.message.yaml;

import com.ca.mfaas.message.core.Message;
import com.ca.mfaas.message.core.MessageLoadException;
import com.ca.mfaas.message.core.MessageService;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class YamlMessageServiceTest {


    @Test
    public void testLoadMessages() {
        MessageService messageService = new YamlMessageService("/test-messages.yml");
        Message message = messageService.createMessage("apiml.common.serviceTimeout", "3000");
        assertEquals("Keys are different", "apiml.common.serviceTimeout", message.getMessageTemplate().getKey());

        message = messageService.createMessage("com.ca.mfaas.test.noArguments");
        assertEquals("Keys are different", "com.ca.mfaas.test.noArguments", message.getMessageTemplate().getKey());
    }


    @Test(expected = MessageLoadException.class)
    public void testLoadMessages_whenFormatIsDifferentInYamlFile() {
        new YamlMessageService("/test-wrong-format-messages.yml");
    }


    @Test(expected = MessageLoadException.class)
    public void testLoadMessages_whenYamlFileIsNotExist() {
        new YamlMessageService("/non-existing-file.yml");
    }

}
