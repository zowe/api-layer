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
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.WebsocketTest;
import org.zowe.apiml.util.config.CloudGatewayConfiguration;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;
import org.zowe.apiml.util.http.HttpClientUtils;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.zowe.apiml.util.requests.Endpoints.DISCOVERABLE_WS_HEADER;
import static org.zowe.apiml.util.requests.Endpoints.DISCOVERABLE_WS_UPPERCASE;

@WebsocketTest
class WebSocketProxyTest implements TestWithStartedInstances {

    private static final URI DC_WS_REST_ENDPOINT = HttpRequestUtils.getUriFromGateway("/discoverableclient/api/v1/ws");

    private static final int WAIT_TIMEOUT_MS = 10000;

    private static final WebSocketHttpHeaders VALID_AUTH_HEADERS = new WebSocketHttpHeaders();
    private static final WebSocketHttpHeaders INVALID_AUTH_HEADERS = new WebSocketHttpHeaders();
    private static final String validToken = "apimlAuthenticationToken=tokenValue";

    private static String getUrl(String scheme, String host, int port, String path) throws URISyntaxException {
        return new URIBuilder()
            .setScheme(scheme.equals("http") ? "ws" : "wss" )
            .setHost(host)
            .setPort(port)
            .setPath(path)
            .build().toString();
    }

    private static Stream<Arguments> gatewayUrls(String gatewayUrl) throws URISyntaxException {
        GatewayServiceConfiguration zuulGateway = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        CloudGatewayConfiguration cloudGateway = ConfigReader.environmentConfiguration().getCloudGatewayConfiguration();

        return Stream.of(
            Arguments.of(getUrl(zuulGateway.getScheme(), zuulGateway.getHost(), zuulGateway.getPort(), gatewayUrl)),
            Arguments.of(getUrl(cloudGateway.getScheme(), cloudGateway.getHost(), cloudGateway.getPort(), gatewayUrl))
        );
    }

    private static Stream<Arguments> discoverableWsHeader() throws URISyntaxException {
        return gatewayUrls(DISCOVERABLE_WS_HEADER);
    }

    private static Stream<Arguments> discoverableWsUppercase() throws URISyntaxException {
        return gatewayUrls(DISCOVERABLE_WS_UPPERCASE);
    }

    private static Stream<Arguments> discoverableWsBad() throws URISyntaxException {
        return gatewayUrls("/discoverableclient/ws/v1/bad");
    }

    private static Stream<Arguments> discoverableWsWrongServiceUppercase() throws URISyntaxException {
        return gatewayUrls("/wrong-service/ws/v1/uppercase");
    }

    private static Stream<Arguments> discoverableWsIncorrectFormat() throws URISyntaxException {
        return gatewayUrls("/ws/wrong");
    }

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

    private WebSocketSession appendingWebSocketSession(String url, WebSocketHttpHeaders headers, StringBuilder response, int countToNotify)
        throws Exception {
        StandardWebSocketClient client = new StandardWebSocketClient();
        client.setSslContext(HttpClientUtils.ignoreSslContext());
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

                    @ParameterizedTest
                    @MethodSource("org.zowe.apiml.integration.proxy.WebSocketProxyTest#discoverableWsUppercase")
                    void message(String url) throws Exception {
                        final StringBuilder response = new StringBuilder();

                        WebSocketSession session = appendingWebSocketSession(url, VALID_AUTH_HEADERS, response, 1);

                        session.sendMessage(new TextMessage("hello world!"));
                        synchronized (response) {
                            response.wait(WAIT_TIMEOUT_MS);
                        }

                        assertEquals("HELLO WORLD!", response.toString());
                        session.close();
                    }

                    @ParameterizedTest
                    @MethodSource("org.zowe.apiml.integration.proxy.WebSocketProxyTest#discoverableWsHeader")
                    void headers(String url) throws Exception {
                        final StringBuilder response = new StringBuilder();
                        if (!VALID_AUTH_HEADERS.containsKey("X-Test")) {
                            VALID_AUTH_HEADERS.add("X-Test", "value");
                        }
                        VALID_AUTH_HEADERS.add("Cookie", validToken);
                        WebSocketSession session = appendingWebSocketSession(url, VALID_AUTH_HEADERS, response, 1);

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

                    @ParameterizedTest
                    @MethodSource("org.zowe.apiml.integration.proxy.WebSocketProxyTest#discoverableWsBad")
                    void whenPathIsNotCorrect(String url) throws Exception {
                        final StringBuilder response = new StringBuilder();
                        appendingWebSocketSession(url, VALID_AUTH_HEADERS, response, 1);

                        synchronized (response) {
                            response.wait(WAIT_TIMEOUT_MS);
                        }

                        System.out.println("Response: " + response.toString());
                        assertEquals(0, response.toString().indexOf("CloseStatus[code=1003,"));
                    }

                    @ParameterizedTest
                    @MethodSource("org.zowe.apiml.integration.proxy.WebSocketProxyTest#discoverableWsWrongServiceUppercase")
                    void whenServiceIsNotCorrect(String url) throws Exception {
                        final StringBuilder response = new StringBuilder();
                        appendingWebSocketSession(url, VALID_AUTH_HEADERS, response, 1);

                        synchronized (response) {
                            response.wait(WAIT_TIMEOUT_MS);
                        }

                        assertEquals("CloseStatus[code=1003, reason=Requested service wrong-service is not known by the gateway]",
                            response.toString());
                    }

                    @ParameterizedTest
                    @MethodSource("org.zowe.apiml.integration.proxy.WebSocketProxyTest#discoverableWsIncorrectFormat")
                    void whenUrlFormatIsNotCorrect(String url) throws Exception {
                        final StringBuilder response = new StringBuilder();
                        appendingWebSocketSession(url, response, 1);

                        synchronized (response) {
                            response.wait(WAIT_TIMEOUT_MS);
                        }

                        assertEquals("CloseStatus[code=1003, reason=Invalid URL format]", response.toString());
                    }

                }

            }

            @Nested
            class WhenInvalid {

                @ParameterizedTest
                @MethodSource("org.zowe.apiml.integration.proxy.WebSocketProxyTest#discoverableWsUppercase")
                void returnError(String url) throws Exception {
                    final StringBuilder response = new StringBuilder();

                    WebSocketSession session = appendingWebSocketSession(url, INVALID_AUTH_HEADERS, response, 1);

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

        @ParameterizedTest
        @MethodSource("org.zowe.apiml.integration.proxy.WebSocketProxyTest#discoverableWsUppercase")
        void getCorrectResponse(String url) throws Exception {
            final StringBuilder response = new StringBuilder();
            WebSocketSession session = appendingWebSocketSession(url, VALID_AUTH_HEADERS, response, 2);

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
