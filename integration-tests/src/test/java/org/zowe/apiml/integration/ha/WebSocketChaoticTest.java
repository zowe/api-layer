/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.ha;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;
import org.springframework.core.annotation.Order;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.ChaoticHATest;
import org.zowe.apiml.util.http.HttpClientUtils;
import org.zowe.apiml.util.requests.ha.HADiscoverableClientRequests;
import org.zowe.apiml.util.requests.ha.HAGatewayRequests;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.tomcat.websocket.Constants.SSL_CONTEXT_PROPERTY;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.zowe.apiml.util.requests.Endpoints.DISCOVERABLE_WS_UPPERCASE;

/**
 * Verify behaviour of the Websocket under HA and chaotic testing
 */
@ChaoticHATest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
class WebSocketChaoticTest implements TestWithStartedInstances {
    private final HAGatewayRequests gatewaysWsRequests = new HAGatewayRequests("wss");
    private final HADiscoverableClientRequests haDiscoverableClientRequests = new HADiscoverableClientRequests();
    private static final int WAIT_TIMEOUT_MS = 10000;

    private static final WebSocketHttpHeaders VALID_AUTH_HEADERS = new WebSocketHttpHeaders();

    @BeforeAll
    static void setup() {
        String plainCred = "user:pass";
        String base64cred = Base64.getEncoder().encodeToString(plainCred.getBytes());
        VALID_AUTH_HEADERS.add("Authorization", "Basic " + base64cred);
    }

    private TextWebSocketHandler appendResponseHandler(StringBuilder target, int countToNotify) {

        final AtomicInteger counter = new AtomicInteger(countToNotify);
        return new TextWebSocketHandler() {
            @Override
            public void handleTextMessage(WebSocketSession session, TextMessage message) {
                synchronized (target) {
                    target.append(message.getPayload());
                    if (counter.decrementAndGet() == 0) {
                        target.notify();
                    }
                }
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                synchronized (target) {
                    target.append(status.toString());
                    if (counter.decrementAndGet() == 0) {
                        target.notify();
                    }
                }
            }
        };
    }

    private WebSocketSession appendingWebSocketSession(URI uri, WebSocketHttpHeaders headers, StringBuilder response, int countToNotify)
        throws Exception {
        StandardWebSocketClient client = new StandardWebSocketClient();
        client.getUserProperties().put(SSL_CONTEXT_PROPERTY, HttpClientUtils.ignoreSslContext());
        return client.doHandshake(appendResponseHandler(response, countToNotify), headers, uri).get(30000, TimeUnit.MILLISECONDS);
    }

    @Nested
    class WhenRoutingSessionGivenHA {

        private WebSocketSession session;

        @AfterEach
        void tearDown() throws IOException {
            if (session != null) {
                session.close();
            }
        }

        @Nested
        class OpeningASession {

            @Nested
            @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
            class WhenAnInstanceIsOff {

                @Test
                @Order(1)
                void propagateTheSessionToTheAliveInstance() throws Exception {
                    final StringBuilder response = new StringBuilder();

                    session = appendingWebSocketSession(gatewaysWsRequests.getGatewayUrl( 0, DISCOVERABLE_WS_UPPERCASE), VALID_AUTH_HEADERS, response, 1);

                    // shutdown one instance of DC to check whether the message can reach out the other instance
                    haDiscoverableClientRequests.shutdown(0);

                    session.sendMessage(new TextMessage("hello world!"));
                    synchronized (response) {
                        response.wait(WAIT_TIMEOUT_MS);
                    }

                    assertEquals("HELLO WORLD!", response.toString());
                }

                @Test
                @Order(2)
                void newSessionCanBeCreated() throws Exception {
                    final StringBuilder response = new StringBuilder();

                    // create websocket session using the second instance of Gateway
                    session = appendingWebSocketSession(gatewaysWsRequests.getGatewayUrl( 1, DISCOVERABLE_WS_UPPERCASE), VALID_AUTH_HEADERS, response, 1);

                    session.sendMessage(new TextMessage("hello world 2!"));
                    synchronized (response) {
                        response.wait(WAIT_TIMEOUT_MS);
                    }

                    assertEquals("HELLO WORLD 2!", response.toString());
                }

                /**
                 * The issue here proves that we don't actually correctly route.
                 * TODO: Introduce HA capabailities for the WebSocket connections.
                 */
                @Nested
                @Order(3)
                class WhenAGatewayInstanceIsOff {

                    @Test
                    @Timeout(value = 30, unit = TimeUnit.SECONDS)
                    void newSessionCanBeCreated() {
                        final StringBuilder response = new StringBuilder();
                        HAGatewayRequests haGatewayRequests = new HAGatewayRequests("https");
                        // take off an instance of Gateway
                        haGatewayRequests.shutdown(0);

                        await("Gateway Shutdown")
                            .atMost(20, TimeUnit.SECONDS)
                            .pollInterval(Duration.ofSeconds(2))
                            .until(() -> {
                                // create websocket session using the second alive instance of Gateway
                                URI gatewayUrl = gatewaysWsRequests.getGatewayUrl( 1, DISCOVERABLE_WS_UPPERCASE);

                                log.error("trying with gatewayUrl: {}", gatewayUrl);

                                session = appendingWebSocketSession(gatewayUrl, VALID_AUTH_HEADERS, response, 1);

                                session.sendMessage(new TextMessage("hello world 2!"));
                                synchronized (response) {
                                    response.wait(WAIT_TIMEOUT_MS);
                                }

                                log.error("Obtained response from {}: {}", gatewayUrl, response.toString());

                                if (response.toString().equals("HELLO WORLD 2!")) {
                                    return true;
                                } else {
                                    session.close();
                                    return false;
                                }
                            });
                    }

                }

            }

        }

    }

}
