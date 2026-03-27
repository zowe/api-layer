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
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.Opcode;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.AfterAll;
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
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.categories.WebsocketTest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;
import org.zowe.apiml.util.http.HttpClientUtils;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.tomcat.websocket.Constants.SSL_CONTEXT_PROPERTY;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.zowe.apiml.util.requests.Endpoints.DISCOVERABLE_WS_HEADER;
import static org.zowe.apiml.util.requests.Endpoints.DISCOVERABLE_WS_TYRUS_ECHO;
import static org.zowe.apiml.util.requests.Endpoints.DISCOVERABLE_WS_UPPERCASE;

@TestsNotMeantForZowe
@WebsocketTest
class WebSocketProxyTest implements TestWithStartedInstances {
    private final GatewayServiceConfiguration gatewayConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
    private static final URI DC_WS_REST_ENDPOINT = HttpRequestUtils.getUriFromGateway("/discoverableclient/api/v1/ws");

    private static final int WAIT_TIMEOUT_MS = 10000;

    private static final WebSocketHttpHeaders VALID_AUTH_HEADERS = new WebSocketHttpHeaders();
    private static final WebSocketHttpHeaders INVALID_AUTH_HEADERS = new WebSocketHttpHeaders();
    private static final String validToken = "apimlAuthenticationToken=tokenValue";

    @BeforeEach
    void setup() {
        VALID_AUTH_HEADERS.clear();
        String plainCred = "user:pass";
        String base64cred = Base64.getEncoder().encodeToString(plainCred.getBytes());
        VALID_AUTH_HEADERS.add("Authorization", "Basic " + base64cred);

        INVALID_AUTH_HEADERS.clear();
        String invalidPlainCred = "user:invalidPass";
        String invalidBase64cred = Base64.getEncoder().encodeToString(invalidPlainCred.getBytes());
        INVALID_AUTH_HEADERS.add("Authorization", "Basic " + invalidBase64cred);
    }

    @AfterAll
    static void teardown() {
        VALID_AUTH_HEADERS.clear();
        INVALID_AUTH_HEADERS.clear();
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
        String scheme = gatewayConfiguration.getScheme().equals("http") ? "ws" : "wss";
        String host = StringUtils.isNotBlank(gatewayConfiguration.getDvipaHost()) ? gatewayConfiguration.getDvipaHost() : gatewayConfiguration.getHost();
        int port = gatewayConfiguration.getPort();

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
                        char[] data = new char[3000];
                        Arrays.fill(data, 'a');
                        for (int i = 0; i < 2; i++) { // at least 6K, more than the original 4K limit
                            VALID_AUTH_HEADERS.add("X-Test-" + i, String.valueOf(data));
                        }
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

                    @Test
                    void whenSendingFrames_andReceivingFrames() throws InterruptedException, URISyntaxException {
                        WebSocketTestClient webSocketClientTyrus = new WebSocketTestClient(new URI(discoverableClientGatewayUrl(DISCOVERABLE_WS_TYRUS_ECHO)));
                        //TODO obtain from SslContext (update Ssl context to expose) webSocketClientTyrus.setSocketFactory();
                        //webSocketClientTyrus.setSocketFactory(SslContext.tlsWithoutCert.getSSLConfig().getSSLSocketFactory());
                        webSocketClientTyrus.messages.clear();
                        boolean connected = webSocketClientTyrus.connectBlocking();
                        assertTrue(connected);

                        byte[] data = RandomUtils.insecure().randomBytes(21_504);
                        ByteBuffer frame1 = ByteBuffer.wrap(ArrayUtils.subarray(data, 0, 8192));
                        ByteBuffer frame2 = ByteBuffer.wrap(ArrayUtils.subarray(data, 8192, 16384));
                        ByteBuffer frame3 = ByteBuffer.wrap(ArrayUtils.subarray(data, 16384, 21504));

                        webSocketClientTyrus.sendFragmentedFrame(Opcode.BINARY, frame1, false);
                        webSocketClientTyrus.sendFragmentedFrame(Opcode.BINARY, frame2, false);
                        webSocketClientTyrus.sendFragmentedFrame(Opcode.BINARY, frame3, true);

                        await()
                            .atMost(Duration.ofSeconds(40))
                            .untilAsserted(() -> {
                                ByteBuffer response = webSocketClientTyrus.getBuffer();
                                assertNotNull(response);
                                assertTrue(response.remaining() > 0);
                                assertTrue(response.capacity() == 21_504, "capacity was " + response.capacity());
                            });

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

                        System.out.println("Response: " + response.toString());
                        assertEquals(0, response.toString().indexOf("CloseStatus[code=1003,"));
                    }

                    @Test
                    void whenServiceIsNotCorrect() throws Exception {
                        final StringBuilder response = new StringBuilder();
                        appendingWebSocketSession(discoverableClientGatewayUrl("/wrong-service/ws/v1/uppercase"), VALID_AUTH_HEADERS, response, 1);

                        synchronized (response) {
                            response.wait(WAIT_TIMEOUT_MS);
                        }

                        assertEquals("CloseStatus[code=1003, reason=Requested service wrong-service is not known by the gateway]",
                            response.toString());
                    }

                    @Test
                    void whenUrlFormatIsNotCorrect() throws Exception {
                        final StringBuilder response = new StringBuilder();
                        appendingWebSocketSession(discoverableClientGatewayUrl("/ws/wrong"), response, 1);

                        synchronized (response) {
                            response.wait(WAIT_TIMEOUT_MS);
                        }

                        assertEquals("CloseStatus[code=1003, reason=Invalid URL format]", response.toString());
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
                            response.wait(WAIT_TIMEOUT_MS);
                        }

                        assertTrue(response.toString().contains("code=1003"), "WebSocket response: " + response + ". Does not contain code=1003");
                        assertTrue(response.toString().contains("UpgradeException"), "WebSocket response: " + response + ". Does not contain \"UpgradeException\"");

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
                .then().body("content",is("Hello, Web service!"))
                .and()
                .statusCode(SC_OK);
        }

    }

    private static class WebSocketTestClient extends WebSocketClient {

        @Getter
        private List<String> messages = new ArrayList<>();

        @Getter
        private ByteBuffer buffer;

        @Setter
        private BiConsumer<Integer, String> onClose;

        public WebSocketTestClient(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            messages.clear();
        }

        @Override
        public void onMessage(String message) {
            messages.add(message);
        }

        @Override
        public void onMessage(ByteBuffer bytes) {
            buffer = bytes;
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            if (onClose != null) {
                onClose.accept(code, reason);
            }
        }

        @Override
        public void onError(Exception ex) {
            fail(ex);
        }

    }

}
