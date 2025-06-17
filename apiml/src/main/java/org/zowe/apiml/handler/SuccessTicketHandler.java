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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Service;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.passticket.UsernameNotProvidedException;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.ticket.TicketRequest;
import org.zowe.apiml.ticket.TicketResponse;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuccessTicketHandler implements ServerAuthenticationSuccessHandler {

    private final ObjectMapper mapper;
    private final PassTicketService passTicketService;
    private final MessageService messageService;

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        var response = webFilterExchange.getExchange().getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);


        return getTicketResponse(webFilterExchange.getExchange().getRequest(), authentication).flatMap(resposne -> {
            var bufferFactory = response.bufferFactory();
            DataBuffer buffer = bufferFactory.wrap(new byte[0]);
            try {
                response.setStatusCode(HttpStatus.OK);
                buffer = bufferFactory.wrap(mapper.writeValueAsBytes(resposne));

            } catch (JsonProcessingException e) {
                response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            }

            return webFilterExchange.getExchange().getResponse().writeWith(Mono.just(buffer));

        }).onErrorResume(PassException.class, exception -> {
            var ex = (IRRPassTicketGenerationException) exception.getCause();
            ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.security.ticket.generateFailed",
                ex.getErrorCode().getMessage()).mapToView();
            return writeResponse(webFilterExchange, response, messageView, HttpStatus.valueOf(ex.getHttpStatus()));
        }).onErrorResume(UsernameNotProvidedException.class, ex -> {
            ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.security.ticket.generateFailed",
                ex.getMessage()).mapToView();
            return writeResponse(webFilterExchange, response, messageView, HttpStatus.INTERNAL_SERVER_ERROR);
        }).onErrorResume(IncorrectRequestBodyException.class, ex -> {
            ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.security.ticket.invalidApplicationName").mapToView();
            return writeResponse(webFilterExchange, response, messageView, HttpStatus.BAD_REQUEST);
        });
    }

    private Mono<Void> writeResponse(WebFilterExchange webFilterExchange, ServerHttpResponse response, ApiMessageView messageView, HttpStatusCode statusCode) {
        var bufferFactory = response.bufferFactory();
        DataBuffer buffer;
        try {
            buffer = bufferFactory.wrap(mapper.writeValueAsBytes(messageView));
        } catch (JsonProcessingException e) {
            throw new IncorrectRequestBodyException("ApplicationName not provided");
        }
        response.setStatusCode(statusCode);
        log.debug("The generation of the PassTicket failed. Please supply a valid user and application name, and check that corresponding permissions have been set up.");
        return webFilterExchange.getExchange().getResponse().writeWith(Mono.just(buffer));
    }

    private Mono<TicketResponse> getTicketResponse(ServerHttpRequest request, Authentication authentication) {
        TokenAuthentication tokenAuthentication = (TokenAuthentication) authentication;
        var userId = tokenAuthentication.getPrincipal();
        return request.getBody().collectList().handle((bufferList, sink) -> {
            var bufferFactory = new DefaultDataBufferFactory();
            int totalSize = bufferList.stream().mapToInt(DataBuffer::readableByteCount).sum();
            DataBuffer mergedBuffer = bufferFactory.allocateBuffer(totalSize);
            bufferList.forEach(mergedBuffer::write); // Write each buffer into the new one

            try {
                String applicationName = mapper.readValue(mergedBuffer.asInputStream(), TicketRequest.class).getApplicationName();
                if (applicationName == null || applicationName.trim().isEmpty()) {
                    sink.error(new IncorrectRequestBodyException("ApplicationName not provided"));
                    return;
                }
                String ticket = passTicketService.generate(userId, applicationName);
                sink.next(new TicketResponse(tokenAuthentication.getCredentials(), userId, applicationName, ticket));
            } catch (PassTicketException e) {
                sink.error(new PassException(e));
            } catch (IOException e) {
                sink.error(new IncorrectRequestBodyException("Cannot parse the passticket body"));
            }

        });
    }
}

class IncorrectRequestBodyException extends RuntimeException {
    public IncorrectRequestBodyException(String message) {
        super(message);
    }
}

class PassException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public PassException(PassTicketException cause) {
        super(cause);
    }
}
