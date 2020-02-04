/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.enable.config;

import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.junit.Test;

import static org.junit.Assert.assertTrue;


public class EnableApiDiscoveryConfigTest {

    @Test
    public void messageServiceDiscovery() {
        String baseUrl = "localhost";
        String ipAddress = "127.0.0.0";
        String discovery = "https://localhost:10011/discovery";
        String correctMessage = String.format(
            "ZWEA001I Registering to API Mediation Layer: {baseUrl=%s, ipAddress=%s, discoveryServiceUrls=%s}",
            baseUrl, ipAddress, discovery);

        MessageService messageService = new EnableApiDiscoveryConfig().messageServiceDiscovery();
        Message message = messageService.createMessage("apiml.enabler.registration.successful",
            baseUrl, ipAddress, discovery);

        assertTrue(message.mapToLogMessage().contains(correctMessage));
    }
}
