/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.websocket;

import jakarta.websocket.ClientEndpointConfig;
import org.apache.tomcat.websocket.Constants;
import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ApimlWebSocketClientTest {

    @Test
    void whenCreateEndpointConfig_thenConfigContainsUserProperties() {
        var wsContainer = mock(WsWebSocketContainer.class);
        var client = new ApimlWebSocketClient(wsContainer);
        var configurator = mock(ClientEndpointConfig.Configurator.class);
        var endpointConfig = client.createEndpointConfig(configurator, Collections.emptyList());
        assertTrue(endpointConfig.getUserProperties().containsKey(Constants.IO_TIMEOUT_MS_PROPERTY));
    }
}
