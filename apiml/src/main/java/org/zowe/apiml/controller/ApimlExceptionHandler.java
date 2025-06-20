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
import org.zowe.apiml.security.common.error.AccessTokenInvalidBodyException;
import org.zowe.apiml.security.common.error.AccessTokenMissingBodyException;
import reactor.core.publisher.Mono;

import static org.apache.http.HttpStatus.*;

@Slf4j
@RestControllerAdvice
public class ApimlExceptionHandler extends GatewayExceptionHandler {

    private static final String GENERATE_FAILED_MESSAGE_KEY = "org.zowe.apiml.security.ticket.generateFailed";

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    public ApimlExceptionHandler(ObjectMapper mapper, MessageService messageService,
                                 LocaleContextResolver localeContextResolver) {
        super(mapper, messageService, localeContextResolver);
    }

    @ExceptionHandler(AccessTokenInvalidBodyException.class)
    public Mono<Void> handleAccessTokenBodyNotValidException(ServerWebExchange exchange, AccessTokenInvalidBodyException
        ex) {
        log.debug("Invalid AccessToken body format, status: {}, message: {}", HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return setBodyResponse(exchange, SC_BAD_REQUEST, "org.zowe.apiml.accessToken.invalidFormat");
    }

    @ExceptionHandler(AccessTokenMissingBodyException.class)
    public Mono<Void> handleAccessTokenMissingBodyException(ServerWebExchange exchange, AccessTokenMissingBodyException
        ex) {
        log.debug("Missing AccessToken body, status: {}, message: {}", HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return setBodyResponse(exchange, SC_BAD_REQUEST, "org.zowe.apiml.security.token.accessTokenBodyMissingScopes");
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
        return setBodyResponse(exchange, SC_INTERNAL_SERVER_ERROR, GENERATE_FAILED_MESSAGE_KEY);
    }

    @ExceptionHandler(PassTicketException.class)
    public Mono<Void> handlePassTicketException(ServerWebExchange exchange, PassTicketException ex) {
        log.debug("PassTicket generation exception: {}", ex.getMessage());
        if (ex.getCause() instanceof IRRPassTicketGenerationException irrEx && irrEx.getCause() != null) {
            var reason = irrEx.getCause().getMessage();
            return setBodyResponse(exchange, SC_INTERNAL_SERVER_ERROR, GENERATE_FAILED_MESSAGE_KEY, reason);
        }
        return setBodyResponse(exchange, SC_INTERNAL_SERVER_ERROR, GENERATE_FAILED_MESSAGE_KEY, ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public Mono<Void> handleBadCredentialsException(ServerWebExchange exchange, BadCredentialsException ex) {
        log.debug("Bad credentials: {}", ex.getMessage());
        return setBodyResponse(exchange, SC_UNAUTHORIZED, "org.zowe.apiml.security.login.invalidCredentials");
    }

}
