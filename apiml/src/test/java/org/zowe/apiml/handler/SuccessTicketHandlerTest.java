/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.ticket.TicketRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SuccessTicketHandlerTest {

    private ObjectMapper objectMapper;
    private PassTicketService passTicketService;
    private SuccessTicketHandler handler;
    private final MessageService messageService = new YamlMessageService("/apiml-log-messages.yml");

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        passTicketService = mock(PassTicketService.class);
        handler = new SuccessTicketHandler(objectMapper, passTicketService, messageService);
    }

    @Test
    void onAuthenticationSuccess_shouldReturnTicketResponse() throws Exception {
        String applId = "applId";
        String user = "user";
        String token = "token";
        String ticket = "passticket";

        TicketRequest request = new TicketRequest();
        request.setApplicationName(applId);
        byte[] bodyBytes = objectMapper.writeValueAsBytes(request);
        DataBuffer buffer = new DefaultDataBufferFactory().wrap(bodyBytes);

        MockServerHttpRequest httpRequest = MockServerHttpRequest.post("/ticket")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(buffer));

        MockServerWebExchange exchange = MockServerWebExchange.from(httpRequest);
        WebFilterExchange webFilterExchange = new WebFilterExchange(exchange, mock(WebFilterChain.class));
        TokenAuthentication auth = new TokenAuthentication(user, token, TokenAuthentication.Type.JWT);

        when(passTicketService.generate(user, applId)).thenReturn(ticket);

        StepVerifier.create(handler.onAuthenticationSuccess(webFilterExchange, auth))
            .verifyComplete();

        StepVerifier.create(exchange.getResponse().getBody())
            .consumeNextWith(dataBuffer -> {
                byte[] responseBytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(responseBytes);
                DataBufferUtils.release(dataBuffer);
                String json = new String(responseBytes, StandardCharsets.UTF_8);
                assert json.contains(user);
                assert json.contains(ticket);
                assert json.contains(applId);
            })
            .verifyComplete();
    }

    @Test
    void onAuthenticationSuccess_missingApplicationName_shouldReturnError() throws Exception {
        TicketRequest request = new TicketRequest();
        byte[] bodyBytes = objectMapper.writeValueAsBytes(request);
        DataBuffer buffer = new DefaultDataBufferFactory().wrap(bodyBytes);

        MockServerHttpRequest httpRequest = MockServerHttpRequest.post("/ticket")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(buffer));

        MockServerWebExchange exchange = MockServerWebExchange.from(httpRequest);
        WebFilterExchange webFilterExchange = new WebFilterExchange(exchange, mock(WebFilterChain.class));
        TokenAuthentication auth = new TokenAuthentication("user", "token", TokenAuthentication.Type.JWT);

        StepVerifier.create(handler.onAuthenticationSuccess(webFilterExchange, auth))
            .verifyComplete();

        StepVerifier.create(exchange.getResponse().getBody())
            .consumeNextWith(dataBuffer -> {
                byte[] responseBytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(responseBytes);
                DataBufferUtils.release(dataBuffer);
                String json = new String(responseBytes, StandardCharsets.UTF_8);
                System.out.println(json);
                assert json.contains("ZWEAG140E");
            })
            .verifyComplete();
    }

    @Test
    void onAuthenticationSuccess_passTicketFailure_shouldReturnError() throws Exception {
        String user = "user";
        String token = "token";
        String applId = "applId";

        TicketRequest request = new TicketRequest();
        request.setApplicationName(applId);
        byte[] bodyBytes = objectMapper.writeValueAsBytes(request);
        DataBuffer buffer = new DefaultDataBufferFactory().wrap(bodyBytes);

        MockServerHttpRequest httpRequest = MockServerHttpRequest.post("/ticket")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(buffer));

        MockServerWebExchange exchange = MockServerWebExchange.from(httpRequest);
        WebFilterExchange webFilterExchange = new WebFilterExchange(exchange, mock(WebFilterChain.class));
        TokenAuthentication auth = new TokenAuthentication(user, token, TokenAuthentication.Type.JWT);

        PassTicketException ex =  new IRRPassTicketGenerationException(8, 16, 32);
        when(passTicketService.generate(user, applId)).thenThrow(ex);

        StepVerifier.create(handler.onAuthenticationSuccess(webFilterExchange, auth))
            .verifyComplete();

        StepVerifier.create(exchange.getResponse().getBody())
            .consumeNextWith(dataBuffer -> {
                byte[] responseBytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(responseBytes);
                DataBufferUtils.release(dataBuffer);
                String json = new String(responseBytes, StandardCharsets.UTF_8);
                assert json.contains("ZWEAG141E");
            })
            .verifyComplete();
    }

}
