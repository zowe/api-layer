/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.zowe.apiml.config.ApplicationInfo;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.product.gateway.GatewayNotAvailableException;
import org.zowe.apiml.security.common.token.*;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Exception handler deals with exceptions (methods listed below) that are thrown during the authentication process
 */
@Slf4j
@Component
public class AuthExceptionHandler extends AbstractExceptionHandler {

    private ApplicationInfo applicationInfo;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public AuthExceptionHandler(
        MessageService messageService,
        ObjectMapper objectMapper,
        @Autowired(required = false) ApplicationInfo applicationInfo) {
            super(messageService, objectMapper);
            this.applicationInfo = applicationInfo == null ? ApplicationInfo.builder().isModulith(false).build() : applicationInfo;
    }

    @AllArgsConstructor
    private static class HandlerContext {
        String requestUri;
        BiConsumer<ApiMessageView, HttpStatus> function;
        BiConsumer<String, String> addHeader;
    }

    @FunctionalInterface
    private interface ExceptionHandler<E extends Exception> {
        void handle(E ex, HandlerContext ctx);
    }

    private <E extends Exception> Map.Entry<Class<E>, ExceptionHandler<E>> entry(Class<E> clazz, ExceptionHandler<E> handler) {
        return Map.entry(clazz, handler);
    }

    private final Map<Class<? extends Exception>, ExceptionHandler<? extends Exception>> exceptionHandlers = Map.ofEntries(
        entry(InsufficientAuthenticationException.class,
            (ex, ctx) -> handleAuthenticationRequired(ctx.requestUri, ctx.function, ctx.addHeader, ex)),
        entry(BadCredentialsException.class,
            (ex, ctx) -> handleBadCredentials(ctx.requestUri, ctx.function, ex)),
        entry(AuthenticationCredentialsNotFoundException.class,
            (ex, ctx) -> handleAuthenticationCredentialsNotFound(ctx.requestUri, ctx.function, ex)),
        entry(AuthMethodNotSupportedException.class,
            (ex, ctx) -> handleAuthMethodNotSupported(ctx.requestUri, ctx.function, ex)),
        entry(TokenNotValidException.class,
            (ex, ctx) -> handleTokenNotValid(ctx.requestUri, ctx.function, ctx.addHeader, ex)),
        entry(NoMainframeIdentityException.class,
            (ex, ctx) -> handleNoMainframeIdentity(ctx.requestUri, ctx.function, ctx.addHeader, ex)),
        entry(TokenNotProvidedException.class,
            (ex, ctx) -> handleTokenNotProvided(ctx.requestUri, ctx.function, ex)),
        entry(TokenExpireException.class,
            (ex, ctx) -> handleTokenExpire(ctx.requestUri, ctx.function, ex)),
        entry(TokenFormatNotValidException.class,
            (ex, ctx) -> handleTokenFormatException(ctx.requestUri, ctx.function, ex)),
        entry(AccessTokenInvalidBodyException.class,
            (ex, ctx) -> handleBadRequest(ctx.requestUri, ctx.function, ex, "org.zowe.apiml.accessToken.invalidFormat")),
        entry(AccessTokenMissingBodyException.class,
            (ex, ctx) -> handleBadRequest(ctx.requestUri, ctx.function, ex, "org.zowe.apiml.security.token.accessTokenBodyMissingScopes")),
        entry(InvalidCertificateException.class,
            (ex, ctx) -> handleInvalidCertificate(ctx.function, ex)),
        entry(ZosAuthenticationException.class,
            (ex, ctx) -> handleZosAuthenticationException(ctx.function, ex)),
        entry(InvalidTokenTypeException.class,
            (ex, ctx) -> handleInvalidTokenTypeException(ctx.requestUri, ctx.function, ex)),
        entry(AuthenticationException.class,
            (ex, ctx) -> handleAuthenticationException(ctx.requestUri, ctx.function, ex)),
        entry(ServiceNotAccessibleException.class,
            (ex, ctx) -> handleServiceNotAccessibleException(ctx.requestUri, ctx.function, ex)),
        entry(NoResourceFoundException.class,
            (ex, ctx) -> handleNoResourceFoundException(ctx.function, ex)),
        entry(RuntimeException.class,
            (ex, ctx) -> handleRuntimeException(ctx.requestUri, ctx.function, ex)),
        entry(WebClientResponseException.BadRequest.class,
            (ex, ctx) -> handleBadRequest(ctx.requestUri, ctx.function, ex, "org.zowe.apiml.security.login.invalidInput")),
        entry(AccessDeniedException.class,
            (ex, ctx) ->  handleForbidden(ctx.function, ex)
        ),
        entry(GatewayNotAvailableException.class,
            (ex, ctx) -> handleGatewayNotAvailable(ctx.function, ex)
        )
    );

    private <E extends Exception> ExceptionHandler<E> resolveHandler(E ex) {
        Class<?> exClass = ex.getClass();
        while (exClass != null) {
            if (!applicationInfo.isModulith() && exClass == RuntimeException.class) {
                return null;
            }

            ExceptionHandler<E> handler = (ExceptionHandler<E>) exceptionHandlers.get(exClass);
            if (handler != null) {
                return handler;
            }
            if (exClass == exClass.getSuperclass()) {
                return null;
            }
            exClass = exClass.getSuperclass();
        }
        return null;
    }

    /**
     * Entry method that takes care about the exception passed to it
     *
     * @param requestUri Http request URI
     * @param function   message function
     * @param addHeader  header
     * @param ex         Exception to be handled
     */
    @Override
    public void handleException(String requestUri,
                                BiConsumer<ApiMessageView, HttpStatus> function,
                                BiConsumer<String, String> addHeader,
                                Exception ex) throws ServletException {

        HandlerContext ctx = new HandlerContext(requestUri, function, addHeader);
        ExceptionHandler<Exception> handler = resolveHandler(ex);

        if (handler != null) {
            handler.handle(ex, ctx);
            return;
        }

        if (!applicationInfo.isModulith()) {
            throw new ServletException(ex);
        }

        handleUnknownHandler(requestUri, function, ex);
    }

    private void handleZosAuthenticationException(BiConsumer<ApiMessageView, HttpStatus> function, ZosAuthenticationException ex) {
        final ApiMessageView message = messageService.createMessage(ex.getPlatformError().errorMessage, ex.getMessage()).mapToView();
        final HttpStatus status = ex.getPlatformError().responseCode;
        function.accept(message, status);
    }

    private void handleAuthenticationRequired(String requestUri, BiConsumer<ApiMessageView, HttpStatus> function, BiConsumer<String, String> addHeader, InsufficientAuthenticationException ex) {
        log.debug(MESSAGE_FORMAT, HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
        String error = this.messageService.createMessage("org.zowe.apiml.zaas.security.schema.missingAuthentication").mapToLogMessage();
        addHeader.accept(ApimlConstants.AUTH_FAIL_HEADER, error);
        writeErrorResponse(ErrorType.AUTH_REQUIRED.getErrorMessageKey(), HttpStatus.UNAUTHORIZED, function, requestUri);
    }

    private void handleBadCredentials(String requestUri, BiConsumer<ApiMessageView, HttpStatus> function, BadCredentialsException ex) {
        log.debug(MESSAGE_FORMAT, HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
        writeErrorResponse(ErrorType.BAD_CREDENTIALS.getErrorMessageKey(), HttpStatus.UNAUTHORIZED, function, requestUri);
    }

    private void handleAuthenticationCredentialsNotFound(String requestUri, BiConsumer<ApiMessageView, HttpStatus> function, AuthenticationCredentialsNotFoundException ex) {
        log.debug(MESSAGE_FORMAT, HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        writeErrorResponse(ErrorType.AUTH_CREDENTIALS_NOT_FOUND.getErrorMessageKey(), HttpStatus.BAD_REQUEST, function, requestUri);
    }

    private void handleAuthMethodNotSupported(String requestUri, BiConsumer<ApiMessageView, HttpStatus> function, AuthMethodNotSupportedException ex) {
        final HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
        log.debug(MESSAGE_FORMAT, status.value(), ex.getMessage());
        final ApiMessageView message = messageService.createMessage(ErrorType.METHOD_NOT_ALLOWED.getErrorMessageKey(), ex.getMessage(), requestUri).mapToView();
        function.accept(message, status);
    }

    private void handleTokenNotValid(String requestUri, BiConsumer<ApiMessageView, HttpStatus> function, BiConsumer<String, String> addHeader, TokenNotValidException ex) {
        log.debug(MESSAGE_FORMAT, HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
        String error = this.messageService.createMessage("org.zowe.apiml.common.unauthorized").mapToLogMessage();
        addHeader.accept(ApimlConstants.AUTH_FAIL_HEADER, error);
        writeErrorResponse(ErrorType.TOKEN_NOT_VALID.getErrorMessageKey(), HttpStatus.UNAUTHORIZED, function, requestUri);
    }

    private void handleNoMainframeIdentity(String requestUri, BiConsumer<ApiMessageView, HttpStatus> function, BiConsumer<String, String> addHeader, NoMainframeIdentityException ex) {
        log.debug(MESSAGE_FORMAT, HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
        addHeader.accept(ApimlConstants.HEADER_OIDC_TOKEN, ex.getToken());
        addHeader.accept(ApimlConstants.AUTH_FAIL_HEADER, ex.getMessage());
        writeErrorResponse(ErrorType.IDENTITY_MAPPING_FAILED.getErrorMessageKey(), HttpStatus.UNAUTHORIZED, function, requestUri);
    }

    private void handleTokenNotProvided(String requestUri, BiConsumer<ApiMessageView, HttpStatus> function, TokenNotProvidedException ex) {
        log.debug(MESSAGE_FORMAT, HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
        writeErrorResponse(ErrorType.TOKEN_NOT_PROVIDED.getErrorMessageKey(), HttpStatus.UNAUTHORIZED, function, requestUri);
    }

    private void handleTokenExpire(String requestUri, BiConsumer<ApiMessageView, HttpStatus> function, TokenExpireException ex) {
        log.debug(MESSAGE_FORMAT, HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
        writeErrorResponse(ErrorType.TOKEN_EXPIRED.getErrorMessageKey(), HttpStatus.UNAUTHORIZED, function, requestUri);
    }

    private void handleInvalidCertificate(BiConsumer<ApiMessageView, HttpStatus> function, InvalidCertificateException ex) {
        function.accept(null, HttpStatus.FORBIDDEN);
        log.debug(MESSAGE_FORMAT, HttpStatus.FORBIDDEN.value(), ex.getMessage());
    }

    private void handleTokenFormatException(String requestUri, BiConsumer<ApiMessageView, HttpStatus> function, TokenFormatNotValidException ex) {
        log.debug(MESSAGE_FORMAT, HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        writeErrorResponse(ErrorType.TOKEN_NOT_VALID.getErrorMessageKey(), HttpStatus.BAD_REQUEST, function, requestUri);
    }

    private void handleInvalidTokenTypeException(String requestUri, BiConsumer<ApiMessageView, HttpStatus> function, InvalidTokenTypeException ex) {
        log.debug(MESSAGE_FORMAT, HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
        writeErrorResponse(ErrorType.INVALID_TOKEN_TYPE.getErrorMessageKey(), HttpStatus.UNAUTHORIZED, function, requestUri);
    }

    private void handleBadRequest(String requestUri, BiConsumer<ApiMessageView, HttpStatus> function, RuntimeException ex, String messageKey) {
        log.debug(MESSAGE_FORMAT, HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        writeErrorResponse(messageKey, HttpStatus.BAD_REQUEST, function, requestUri);
    }

    private void handleAuthenticationException(String uri, BiConsumer<ApiMessageView, HttpStatus> function, AuthenticationException ex) {
        final ApiMessageView message = messageService.createMessage(ErrorType.AUTH_GENERAL.getErrorMessageKey(), ex.getMessage(), uri).mapToView();
        final HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        log.debug(MESSAGE_FORMAT, status.value(), ex.getMessage());
        function.accept(message, status);
    }

    private void handleUnknownHandler(String uri, BiConsumer<ApiMessageView, HttpStatus> function, Exception ex) {
        // TODO: it should be a general message, this is just for back-compatibility
        final ApiMessageView message = messageService.createMessage(ErrorType.AUTH_GENERAL.getErrorMessageKey(), ex.getMessage(), uri).mapToView();
        final HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        log.debug(MESSAGE_FORMAT, status.value(), ex.getMessage());
        function.accept(message, status);
    }

    private void handleServiceNotAccessibleException(String uri, BiConsumer<ApiMessageView, HttpStatus> function, ServiceNotAccessibleException ex) {
        final ApiMessageView message = messageService.createMessage(ErrorType.SERVICE_UNAVAILABLE.getErrorMessageKey(), ex.getMessage(), uri).mapToView();
        final HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        log.debug(MESSAGE_FORMAT, status.value(), ex.getMessage());
        function.accept(message, status);
    }

    private void handleNoResourceFoundException(BiConsumer<ApiMessageView, HttpStatus> function, NoResourceFoundException ex) {
        log.debug(MESSAGE_FORMAT, HttpStatus.NOT_FOUND.value(), ex.getMessage());
        writeErrorResponse("org.zowe.apiml.common.notFound", HttpStatus.NOT_FOUND, function);
    }

    private void handleRuntimeException(String uri, BiConsumer<ApiMessageView, HttpStatus> function, RuntimeException ex) {
        log.debug(MESSAGE_FORMAT, HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage());
        writeErrorResponse("org.zowe.apiml.common.internalRequestError", HttpStatus.INTERNAL_SERVER_ERROR, function, uri, ExceptionUtils.getMessage(ex), ExceptionUtils.getRootCauseMessage(ex));
    }

    private void handleForbidden(BiConsumer<ApiMessageView, HttpStatus> function, AccessDeniedException ex) {
        log.debug(MESSAGE_FORMAT, HttpStatus.FORBIDDEN.value(), ex.getMessage());
        writeErrorResponse("org.zowe.apiml.security.forbidden", HttpStatus.FORBIDDEN, function);
    }

    private void handleGatewayNotAvailable(BiConsumer<ApiMessageView, HttpStatus> function, GatewayNotAvailableException ex) {
        log.debug(MESSAGE_FORMAT, HttpStatus.SERVICE_UNAVAILABLE.value(), ex.getMessage());
        writeErrorResponse("org.zowe.apiml.security.gatewayNotAvailable", HttpStatus.SERVICE_UNAVAILABLE, function);

    }

}
