/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.ws;

import com.ca.mfaas.util.categories.WebsocketTest;
import com.ca.mfaas.util.config.ConfigReader;
import com.ca.mfaas.util.config.GatewayServiceConfiguration;
import com.ca.mfaas.util.http.HttpClientUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.tomcat.websocket.Constants.SSL_CONTEXT_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WebSocketProxyTest {
    private GatewayServiceConfiguration serviceConfiguration;

    private final static int WAIT_TIMEOUT_MS = 10000;
    private final static String UPPERCASE_URL = "/ws/v1/discoverableclient/uppercase";
    private final static String HEADER_URL = "/ws/v1/discoverableclient/header";

    @Before
    public void setUp() {
        serviceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
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

    private String discoverableClientGatewayUrl(String gatewayUrl) throws URISyntaxException {
        String scheme = serviceConfiguration.getScheme().equals("http") ? "ws" : "wss";
        String host = serviceConfiguration.getHost();
        int port = serviceConfiguration.getPort();

        return new URIBuilder().setScheme(scheme).setHost(host).setPort(port).setPath(gatewayUrl).build().toString();
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

    @Test
    @Category(WebsocketTest.class)
    public void shouldRouteWebSocketSession() throws Exception {
        final StringBuilder response = new StringBuilder();
        WebSocketSession session = appendingWebSocketSession(discoverableClientGatewayUrl(UPPERCASE_URL), response, 1);

        session.sendMessage(new TextMessage("hello world!"));
        synchronized (response) {
            response.wait(WAIT_TIMEOUT_MS);
        }

        assertEquals("HELLO WORLD!", response.toString());
        session.close();
    }

    @Test
    @Category(WebsocketTest.class)
    public void shouldRouteHeaders() throws Exception {
        final StringBuilder response = new StringBuilder();
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("X-Test", "value");
        WebSocketSession session = appendingWebSocketSession(discoverableClientGatewayUrl(HEADER_URL), headers, response, 1);

        session.sendMessage(new TextMessage("gimme those headers"));
        synchronized (response) {
            response.wait(WAIT_TIMEOUT_MS);
        }

        assertTrue(response.toString().contains("x-test:[value]"));
        session.sendMessage(new TextMessage("bye"));
        session.close();
    }

    @Test
    @Category(WebsocketTest.class)
    public void shouldCloseSessionAfterClientServerCloses() throws Exception {
        final StringBuilder response = new StringBuilder();
        WebSocketSession session = appendingWebSocketSession(discoverableClientGatewayUrl(UPPERCASE_URL), response, 2);

        session.sendMessage(new TextMessage("bye"));
        synchronized (response) {
            response.wait(WAIT_TIMEOUT_MS);
        }

        assertEquals("BYECloseStatus[code=1000, reason=null]", response.toString());
    }

    @Test
    @Category(WebsocketTest.class)
    public void shouldFailIfPathIsNotCorrect() throws Exception {
        final StringBuilder response = new StringBuilder();
        appendingWebSocketSession(discoverableClientGatewayUrl(UPPERCASE_URL + "bad"), response, 1);

        synchronized (response) {
            response.wait(WAIT_TIMEOUT_MS);
        }

        System.out.println("Response: " + response.toString());
        assertEquals(0, response.toString().indexOf("CloseStatus[code=1003,"));
    }

    @Test
    @Category(WebsocketTest.class)
    public void shouldFailIfServiceIsNotCorrect() throws Exception {
        final StringBuilder response = new StringBuilder();
        WebSocketSession session = appendingWebSocketSession(
                discoverableClientGatewayUrl("/ws/v1/wrong-service/uppercase"), response, 1);

        synchronized (response) {
            response.wait(WAIT_TIMEOUT_MS);
        }

        assertEquals("CloseStatus[code=1003, reason=Requested service wrong-service is not known by the gateway]",
                response.toString());
    }

    @Test
    @Category(WebsocketTest.class)
    public void shouldFailIfUrlFormatIsNotCorrent() throws Exception {
        final StringBuilder response = new StringBuilder();
        appendingWebSocketSession(discoverableClientGatewayUrl("/ws/wrong"), response, 1);

        synchronized (response) {
            response.wait(WAIT_TIMEOUT_MS);
        }

        assertEquals("CloseStatus[code=1003, reason=Invalid URL format]", response.toString());
    }

}
