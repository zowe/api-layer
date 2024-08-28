/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.proxy;

import io.restassured.RestAssured;
import jakarta.websocket.ContainerProvider;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.WebsocketTest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;
import org.zowe.apiml.util.http.HttpClientUtils;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.zowe.apiml.util.requests.Endpoints.DISCOVERABLE_WS_HEADER;
import static org.zowe.apiml.util.requests.Endpoints.DISCOVERABLE_WS_UPPERCASE;

@WebsocketTest
class WebSocketProxyTest implements TestWithStartedInstances {
    private final GatewayServiceConfiguration serviceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
    private static final URI DC_WS_REST_ENDPOINT = HttpRequestUtils.getUriFromGateway("/discoverableclient/api/v1/ws");

    private static final int WAIT_TIMEOUT_MS = 10000;

    private static final WebSocketHttpHeaders VALID_AUTH_HEADERS = new WebSocketHttpHeaders();
    private static final WebSocketHttpHeaders INVALID_AUTH_HEADERS = new WebSocketHttpHeaders();
    private static final String validToken = "apimlAuthenticationToken=tokenValue";


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

            @Override
            public boolean supportsPartialMessages() {
                return true;
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
        var wsContainer = ContainerProvider.getWebSocketContainer();
        wsContainer.setDefaultMaxTextMessageBufferSize(Integer.MAX_VALUE);
        wsContainer.setDefaultMaxBinaryMessageBufferSize(Integer.MAX_VALUE);
        wsContainer.setAsyncSendTimeout(10_000_000L);
        wsContainer.setDefaultMaxSessionIdleTimeout(10_000_000L);
        StandardWebSocketClient client = new StandardWebSocketClient();

        client.setSslContext(HttpClientUtils.ignoreSslContext());
        URI uri = UriComponentsBuilder.fromUriString(url).build().encode().toUri();
        return client.execute(appendResponseHandler(response, countToNotify), headers, uri).get(30000, TimeUnit.MILLISECONDS);
    }


    @Nested
    class WhenRoutingSession {
        @Nested
        class Authentication {
            @Nested
            class WhenValid {
                @Nested
                class ReturnSuccess {
                    @Test
                    void message() throws Exception {
                        final StringBuilder response = new StringBuilder();

                        WebSocketSession session = appendingWebSocketSession(discoverableClientGatewayUrl(DISCOVERABLE_WS_UPPERCASE), VALID_AUTH_HEADERS, response, 1);

                        session.sendMessage(new TextMessage("hello world!"));
                        synchronized (response) {
                            response.wait(WAIT_TIMEOUT_MS);
                        }

                        assertEquals("HELLO WORLD!", response.toString());
                        session.close();
                    }

                    @Test
                    void headers() throws Exception {
                        final StringBuilder response = new StringBuilder();
                        if (!VALID_AUTH_HEADERS.containsKey("X-Test")) {
                            VALID_AUTH_HEADERS.add("X-Test", "value");
                        }
                        VALID_AUTH_HEADERS.add("Cookie", validToken);
                        WebSocketSession session = appendingWebSocketSession(discoverableClientGatewayUrl(DISCOVERABLE_WS_HEADER), VALID_AUTH_HEADERS, response, 1);

                        session.sendMessage(new TextMessage("gimme those headers"));
                        synchronized (response) {
                            response.wait(WAIT_TIMEOUT_MS);
                        }

                        assertTrue(response.toString().contains("x-test:\"value\""));
                        assertTrue(response.toString().contains(validToken));
                        session.sendMessage(new TextMessage("bye"));
                        session.close();
                    }
                }

                @Nested
                class ReturnError {
                    @Test
                    void whenPathIsNotCorrect() throws Exception {
                        String path = "/discoverableclient/ws/v1/bad";
                        final StringBuilder response = new StringBuilder();
                        appendingWebSocketSession(discoverableClientGatewayUrl(path), VALID_AUTH_HEADERS, response, 1);

                        synchronized (response) {
                            response.wait(WAIT_TIMEOUT_MS);
                        }

                        assertEquals(0, response.toString().indexOf("CloseStatus[code=1011,"), "" +
                            "Expected response with `CloseStatus[code=1011,`, but was `" + response + "`"
                        );
                    }

                    @Test
                    void whenServiceIsNotCorrect() throws Exception {
                        var exception = assertThrows(ExecutionException.class, () -> appendingWebSocketSession(discoverableClientGatewayUrl("/wrong-service/ws/v1/uppercase"), VALID_AUTH_HEADERS, new StringBuilder(), 1));

                        assertEquals("jakarta.websocket.DeploymentException: The HTTP response from the server [404] did not permit the HTTP upgrade to WebSocket",
                            exception.getMessage());
                    }

                    @Test
                    void whenUrlFormatIsNotCorrect() throws Exception {

                        var exception = assertThrows(ExecutionException.class, () -> appendingWebSocketSession(discoverableClientGatewayUrl("/ws/wrong"), VALID_AUTH_HEADERS, new StringBuilder(), 1));

                        assertEquals("jakarta.websocket.DeploymentException: The HTTP response from the server [404] did not permit the HTTP upgrade to WebSocket",
                            exception.getMessage());
                    }

                    @Test
                    void whenHandshakeRequestIsTooLarge() throws Exception {
                        final StringBuilder response = new StringBuilder();
                        if (!VALID_AUTH_HEADERS.containsKey("X-Test")) {
                            VALID_AUTH_HEADERS.add("X-Test", "value");
                        }
                        VALID_AUTH_HEADERS.add("Cookie", validToken);

                        char[] data = new char[3000];
                        Arrays.fill(data, 'a');
                        for (int i = 0; i < 3; i++) {
                            VALID_AUTH_HEADERS.add("X-Test-" + i, String.valueOf(data));
                        }
                        WebSocketSession session = appendingWebSocketSession(discoverableClientGatewayUrl(DISCOVERABLE_WS_HEADER), VALID_AUTH_HEADERS, response, 1);

                        session.sendMessage(new TextMessage("gimme those headers"));
                        synchronized (response) {
                            response.wait();
                        }


                        assertTrue(response.toString().contains("x-test-2"), "WebSocket response: " + response + ". Does not contain x-test-2");
                        session.close();

                    }
                }
            }

            @Nested
            class WhenInvalid {

                @Test
                void returnError() throws Exception {
                    final StringBuilder response = new StringBuilder();

                    WebSocketSession session = appendingWebSocketSession(discoverableClientGatewayUrl(DISCOVERABLE_WS_UPPERCASE), INVALID_AUTH_HEADERS, response, 1);

                    session.sendMessage(new TextMessage("hello world!"));
                    synchronized (response) {
                        response.wait(WAIT_TIMEOUT_MS);
                    }

                    assertEquals("CloseStatus[code=1003, reason=Invalid login credentials]", response.toString());
                    session.close();
                }
            }
        }

    }

    @Nested
    class WhenClosingSession {
        @Test
        void getCorrectResponse() throws Exception {
            final StringBuilder response = new StringBuilder();
            WebSocketSession session = appendingWebSocketSession(discoverableClientGatewayUrl(DISCOVERABLE_WS_UPPERCASE), VALID_AUTH_HEADERS, response, 2);

            session.sendMessage(new TextMessage("bye"));
            synchronized (response) {
                response.wait(WAIT_TIMEOUT_MS);
            }

            assertEquals("BYECloseStatus[code=1000, reason=null]", response.toString());
        }
    }

    @Nested
    class WhenRequestIsNotForWebSocket {

        @BeforeEach
        void beforeClass() {
            RestAssured.useRelaxedHTTPSValidation();
        }

        @Test
        void getGreetingFromREST() {
            given()
                .get(DC_WS_REST_ENDPOINT)
                .then().body("content", is("Hello, Web service!"))
                .and()
                .statusCode(SC_OK);
        }
    }


}
