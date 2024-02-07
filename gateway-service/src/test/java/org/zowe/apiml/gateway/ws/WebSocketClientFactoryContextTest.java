/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ws;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
    properties = {
        "server.webSocket.maxIdleTimeout",
        "server.webSocket.connectTimeout",
        "server.webSocket.stopTimeout",
        "server.webSocket.asyncWriteTimeout",
    },
    classes = { WebSocketClientFactory.class }
)
@ActiveProfiles("WebSocketClientFactoryContextTest")
public class WebSocketClientFactoryContextTest {

    @Autowired
    private WebSocketClientFactory webSocketClientFactory;

    @Nested
    class GivenWebSocketClientParametrization {

        @Test
        void thenBeanIsInitialized() {
            // verify the fields in the autowired bean
            // verify the values in the configured client.
            assertNotNull(webSocketClientFactory);
        }

    }

    @TestConfiguration
    @Profile("WebSocketClientFactoryContextTest")
    public static class Config {

        @Bean
        SslContextFactory.Client jettyClientSslContextFactory() {
            return null;
        }

    }
}
