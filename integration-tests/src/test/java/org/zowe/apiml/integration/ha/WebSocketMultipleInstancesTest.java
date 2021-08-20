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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.ChaoticHATest;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.categories.WebsocketTest;
import org.zowe.apiml.util.http.HttpClientUtils;
import org.zowe.apiml.util.requests.ha.HADiscoverableClientRequests;
import org.zowe.apiml.util.requests.ha.HAGatewayRequests;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.tomcat.websocket.Constants.SSL_CONTEXT_PROPERTY;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestsNotMeantForZowe
@WebsocketTest
@ChaoticHATest
class WebSocketMultipleInstancesTest implements TestWithStartedInstances {
    private final HAGatewayRequests haGatewayRequests = new HAGatewayRequests("wss");
    private final HADiscoverableClientRequests haDiscoverableClientRequests = new HADiscoverableClientRequests();
    private static final int WAIT_TIMEOUT_MS = 10000;

    private static final WebSocketHttpHeaders VALID_AUTH_HEADERS = new WebSocketHttpHeaders();
    private static final WebSocketHttpHeaders INVALID_AUTH_HEADERS = new WebSocketHttpHeaders();

    @BeforeAll
    static void setup() {
        String plainCred = "user:pass";
        String base64cred = Base64.getEncoder().encodeToString(plainCred.getBytes());
        VALID_AUTH_HEADERS.add("Authorization", "Basic " + base64cred);

        String invalidPlainCred = "user:invalidPass";
        String invalidBase64cred = Base64.getEncoder().encodeToString(invalidPlainCred.getBytes());
        INVALID_AUTH_HEADERS.add("Authorization", "Basic " + invalidBase64cred);

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

    private String discoverableClientGatewayUrl(String gatewayUrl, int index) throws URISyntaxException {
        return haGatewayRequests.gatewayServices.get(index).getGatewayUriWithPath(gatewayUrl).toString();
    }

    private WebSocketSession appendingWebSocketSession(String url, WebSocketHttpHeaders headers, StringBuilder response, int countToNotify)
        throws Exception {
        StandardWebSocketClient client = new StandardWebSocketClient();
        client.getUserProperties().put(SSL_CONTEXT_PROPERTY, HttpClientUtils.ignoreSslContext());
        URI uri = UriComponentsBuilder.fromUriString(url).build().encode().toUri();
        return client.doHandshake(appendResponseHandler(response, countToNotify), headers, uri).get(30000, TimeUnit.MILLISECONDS);
    }

    private WebSocketSession appendingWebSocketSession(String url, StringBuilder response, int countToNotify)
        throws Exception {
        return appendingWebSocketSession(url, null, response, countToNotify);
    }


    @Nested
    class WhenRoutingSessionGivenHA {
        @Nested
        class OpeningASession {
            @Nested
            class WhenAnInstanceIsOff {
                @ParameterizedTest(name = "WhenRoutingSessionGivenHA.OpeningASession.WhenAnInstanceIsOff#propagateTheSessionToTheAliveInstance {0}")
                @ValueSource(strings = {"/discoverableclient/ws/v1/uppercase", "/ws/v1/discoverableclient/uppercase"})
                void propagateTheSessionToTheAliveInstance(String path) throws Exception {
                    final StringBuilder response = new StringBuilder();

                    WebSocketSession session = appendingWebSocketSession(discoverableClientGatewayUrl(path, 0), VALID_AUTH_HEADERS, response, 1);

                    //shutdown one instance of DC to check whether the message can reach out the other instance
                    haDiscoverableClientRequests.shutdown(0);

                    session.sendMessage(new TextMessage("hello world!"));
                    synchronized (response) {
                        response.wait(WAIT_TIMEOUT_MS);
                    }

                    assertEquals("HELLO WORLD!", response.toString());
                    session.close();
                }

                @ParameterizedTest(name = "WhenRoutingSessionGivenHA.OpeningASession.WhenAnInstanceIsOff#newSessionCanBeCreated {0}")
                @ValueSource(strings = {"/discoverableclient/ws/v1/uppercase", "/ws/v1/discoverableclient/uppercase"})
                void newSessionCanBeCreated(String path) throws Exception {
                    final StringBuilder response = new StringBuilder();

                    // Create websocket session using the second instance of Gateway
                    WebSocketSession session = appendingWebSocketSession(discoverableClientGatewayUrl(path, 1), VALID_AUTH_HEADERS, response, 1);

                    session.sendMessage(new TextMessage("hello world 2!"));
                    synchronized (response) {
                        response.wait(WAIT_TIMEOUT_MS);
                    }

                    assertEquals("HELLO WORLD 2!", response.toString());
                    session.close();
                }
            }
        }
    }

}
