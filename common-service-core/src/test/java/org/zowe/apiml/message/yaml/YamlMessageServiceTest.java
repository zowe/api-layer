/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.message.yaml;

import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageLoadException;
import org.zowe.apiml.message.core.MessageService;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class YamlMessageServiceTest {


    @Test
    public void testLoadMessages() {
        MessageService messageService = new YamlMessageService("/test-messages.yml");
        Message message = messageService.createMessage("org.zowe.apiml.common.serviceTimeout", "3000");
        assertEquals("Keys are different", "org.zowe.apiml.common.serviceTimeout", message.getMessageTemplate().getKey());

        message = messageService.createMessage("org.zowe.apiml.test.noArguments");
        assertEquals("Keys are different", "org.zowe.apiml.test.noArguments", message.getMessageTemplate().getKey());
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
