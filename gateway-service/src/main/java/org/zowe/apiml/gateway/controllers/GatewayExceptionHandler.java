/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.Assert;
import org.springframework.web.HttpMediaTypeException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.i18n.LocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.zowe.apiml.gateway.service.ZaasServiceIsNotAvailableException;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.apache.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GatewayExceptionHandler {

    private static final String WWW_AUTHENTICATE = "WWW-Authenticate";
    private static String WWW_AUTHENTICATE_FORMAT = "Basic realm=\"%s\"";
    private static final String DEFAULT_REALM = "Realm";

    private final ObjectMapper mapper;
    private final MessageService messageService;
    private final LocaleContextResolver localeContextResolver;

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    private static String createHeaderValue(String realm) {
        Assert.notNull(realm, "realm cannot be null");
        return String.format(WWW_AUTHENTICATE_FORMAT, realm);
    }

    public Mono<Void> setBodyResponse(ServerWebExchange exchange, int responseCode, String messageCode, Object...args) {
        var sessionManager = new DefaultWebSessionManager();
        var serverCodecConfigurer = ServerCodecConfigurer.create();

        var serverWebExchange = new DefaultServerWebExchange(exchange.getRequest(), exchange.getResponse(), sessionManager, serverCodecConfigurer, localeContextResolver);
        serverWebExchange.getResponse().setRawStatusCode(responseCode);
        serverWebExchange.getResponse().getHeaders().add(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE);

        Message message = messageService.createMessage(messageCode, args);
        try {
            DataBuffer buffer = serverWebExchange.getResponse().bufferFactory().wrap(mapper.writeValueAsBytes(message.mapToView()));
            return serverWebExchange.getResponse().writeWith(Flux.just(buffer));
        } catch (JsonProcessingException e) {
            apimlLog.log("org.zowe.apiml.security.errorWritingResponse", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void setWwwAuthenticateResponse(ServerWebExchange exchange) {
        exchange.getResponse().getHeaders().add(WWW_AUTHENTICATE, createHeaderValue(DEFAULT_REALM));
    }

    @ExceptionHandler(WebClientResponseException.BadRequest.class)
    public Mono<Void> handleBadRequestException(ServerWebExchange exchange, WebClientResponseException.BadRequest ex) {
        log.debug("Invalid request structure on {}: {}", exchange.getRequest().getURI(), ex.getMessage());
        return setBodyResponse(exchange, SC_BAD_REQUEST, "org.zowe.apiml.common.badRequest");
    }

    @ExceptionHandler(AuthenticationException.class)
    public Mono<Void> handleAuthenticationException(ServerWebExchange exchange, AuthenticationException ex) {
        log.debug("Unauthorized access on {}: {}", exchange.getRequest().getURI(), ex.getMessage());
        setWwwAuthenticateResponse(exchange);
        return setBodyResponse(exchange, SC_UNAUTHORIZED, "org.zowe.apiml.common.unauthorized");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Mono<Void> handleAccessDeniedException(ServerWebExchange exchange, AccessDeniedException ex) {
        log.debug("Unauthenticated access on {}: {}", exchange.getRequest().getURI(), ex.getMessage());
        return setBodyResponse(exchange, SC_FORBIDDEN, "org.zowe.apiml.security.forbidden");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Mono<Void> handleNoResourceFoundException(ServerWebExchange exchange, NoResourceFoundException ex) {
        log.debug("Resource {} not found: {}", exchange.getRequest().getURI(), ex.getMessage());
        return setBodyResponse(exchange, SC_NOT_FOUND, "org.zowe.apiml.security.notFound");
    }

    @ExceptionHandler(MethodNotAllowedException.class)
    public Mono<Void> handleMethodNotAllowedException(ServerWebExchange exchange, MethodNotAllowedException ex) {
        log.debug("Method not allowed on {}: {}", exchange.getRequest().getURI(), ex.getMessage());
        return setBodyResponse(exchange, SC_METHOD_NOT_ALLOWED, "org.zowe.apiml.common.methodNotAllowed");
    }

    @ExceptionHandler(HttpMediaTypeException.class)
    public Mono<Void> handleHttpMediaTypeException(ServerWebExchange exchange, HttpMediaTypeException ex) {
        log.debug("Invalid media type on {}: {}", exchange.getRequest().getURI(), ex.getMessage());
        return setBodyResponse(exchange, SC_UNSUPPORTED_MEDIA_TYPE, "org.zowe.apiml.common.unsupportedMediaType");
    }

    @ExceptionHandler(Exception.class)
    public Mono<Void> handleInternalError(ServerWebExchange exchange, Exception ex) {
        if (log.isDebugEnabled()) {
            log.debug("Unhandled internal error on {}", ex, exchange.getRequest().getURI());
        } else {
            log.debug("Unhandled internal error on {}: {}", exchange.getRequest().getURI(), ex.getMessage());
        }
        return setBodyResponse(exchange, SC_INTERNAL_SERVER_ERROR, "org.zowe.apiml.common.internalServerError");
    }

    @ExceptionHandler(ServiceNotAccessibleException.class)
    public Mono<Void> handleServiceNotAccessibleException(ServerWebExchange exchange, ZaasServiceIsNotAvailableException ex) {
        log.debug("A service is not available at the moment to finish request {}: {}", exchange.getRequest().getURI(), ex.getMessage());
        return setBodyResponse(exchange, SC_UNSUPPORTED_MEDIA_TYPE, "org.zowe.apiml.common.serviceUnavailable");
    }

}
