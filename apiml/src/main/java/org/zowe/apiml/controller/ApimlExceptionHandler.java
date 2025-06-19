/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.i18n.LocaleContextResolver;
import org.zowe.apiml.gateway.controllers.GatewayExceptionHandler;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketException;
import org.zowe.apiml.passticket.UsernameNotProvidedException;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.error.AccessTokenBodyNotValidException;
import reactor.core.publisher.Mono;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

@Slf4j
@RestControllerAdvice
public class ApimlExceptionHandler extends GatewayExceptionHandler {

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    public ApimlExceptionHandler(ObjectMapper mapper, MessageService messageService,
            LocaleContextResolver localeContextResolver) {
        super(mapper, messageService, localeContextResolver);
    }

    @ExceptionHandler(AccessTokenBodyNotValidException.class)
    public Mono<Void> handleAccessTokenBodyNotValidException(ServerWebExchange exchange, AccessTokenBodyNotValidException
     ex) {
        log.debug("Invalid AccessToken body format, status: {}, message: {}", HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return setBodyResponse(exchange, SC_BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidWebFingerConfigurationException.class)
    public Mono<Void> handleInvalidWebFingerConfigurationException(ServerWebExchange exchange, InvalidWebFingerConfigurationException ex) {
        log.debug("Error while reading webfinger configuration from source.", ex);
        return setBodyResponse(exchange, SC_INTERNAL_SERVER_ERROR, "org.zowe.apiml.security.oidc.invalidWebfingerConfiguration");
    }

    @ExceptionHandler(IncorrectPassTicketRequestBodyException.class)
    public Mono<Void> handleIncorrectPassTicketRequestBodyException(ServerWebExchange exchange, IncorrectPassTicketRequestBodyException ex) {
        log.debug("Incorrect passticket request body received: {}", ex.getMessage());
        return setBodyResponse(exchange, SC_BAD_REQUEST, "org.zowe.apiml.security.ticket.invalidApplicationName");
    }

    @ExceptionHandler(SafAccessDeniedException.class)
    public Mono<Void> handleSafAccessDeniedException(ServerWebExchange exchange, SafAccessDeniedException ex) {
        log.debug("Access denied: {}", ex.getMessage());
        return setBodyResponse(exchange, SC_UNAUTHORIZED, "org.zowe.apiml.security.unauthorized", String.valueOf(ex.getPrincipal()));
    }

    @ExceptionHandler(UsernameNotProvidedException.class)
    public Mono<Void> handleUsernameNotProvidedException(ServerWebExchange exchange, UsernameNotProvidedException ex) {
        log.debug("Username not provided in PassTicket generation: {}", ex.getMessage());
        return setBodyResponse(exchange, SC_INTERNAL_SERVER_ERROR, "org.zowe.apiml.security.ticket.generateFailed");
    }

    @ExceptionHandler(PassTicketException.class)
    public Mono<Void> handlePassTicketException(ServerWebExchange exchange, PassTicketException ex) {
        log.debug("PassTicket generation exception: {}", ex.getMessage());
        if (ex.getCause() instanceof IRRPassTicketGenerationException irrEx && irrEx.getCause() != null) {
            var reason = irrEx.getCause().getMessage();
            return setBodyResponse(exchange, SC_INTERNAL_SERVER_ERROR, "org.zowe.apiml.security.ticket.generateFailed", reason);
        }
        return setBodyResponse(exchange, SC_INTERNAL_SERVER_ERROR, "org.zowe.apiml.security.ticket.generateFailed", ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public Mono<Void> handleBadCredentialsException(ServerWebExchange exchange, BadCredentialsException ex) {
        log.debug("Bad credentials: {}", ex.getMessage());
        return setBodyResponse(exchange, SC_UNAUTHORIZED, "org.zowe.apiml.security.login.invalidCredentials");
    }

}
