/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config.oidc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.gateway.config.InvalidForwardException;
import org.zowe.apiml.gateway.controllers.GatewayExceptionHandler;
import org.zowe.apiml.product.opentelemetry.OtelRequestContext;
import reactor.core.publisher.Mono;

import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Terminates failed OIDC logins without starting another authorization request. */
@Slf4j
@RequiredArgsConstructor
public class OidcAuthenticationFailureHandler implements ServerAuthenticationFailureHandler {

    private final Supplier<GatewayExceptionHandler> exceptionHandler;
    private final Consumer<WebFilterExchange> clearCookies;

    @Override
    public Mono<Void> onAuthenticationFailure(WebFilterExchange exchange, AuthenticationException exception) {
        return handleFailure(exchange, exception);
    }

    /**
     * Also handles OAuth2 and transport errors that Spring does not wrap in AuthenticationException.
     * Any other error (e.g. a bug in the success handler) is propagated to the default error handling.
     */
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return Mono.defer(() -> chain.filter(exchange))
            .onErrorResume(
                exception -> isOidcFailure(exception) && !exchange.getResponse().isCommitted(),
                exception -> handleFailure(new WebFilterExchange(exchange, chain), exception)
            );
    }

    private Mono<Void> handleFailure(WebFilterExchange webFilterExchange, Throwable exception) {
        var exchange = webFilterExchange.getExchange();
        clearCookies.accept(webFilterExchange);
        var failure = classify(exception);
        var requestId = exchange.getRequest().getId();
        log.debug("OIDC login failed (request ID {}): {}, exception type {}",
            requestId, failure.getMessageKey(), exception.getClass().getSimpleName());
        log.debug("OIDC login failure details (request ID {})", requestId, exception);
        var handler = exceptionHandler.get();
        var message = handler.getMessageService().createMessage(failure.getMessageKey(), requestId);
        OtelRequestContext.of(exchange).authenticationFailed()
            .authErrorType(exception.getClass().getName()).authErrorMessage(message.mapToLogMessage());
        exchange.getResponse().getHeaders().set("X-Request-ID", requestId);
        return handler.setBodyResponse(exchange, failure.status, failure.getMessageKey(), requestId);
    }

    private static boolean isOidcFailure(Throwable exception) {
        if (exception instanceof InvalidForwardException) {
            return false;
        }
        return causes(exception).stream().anyMatch(cause ->
            cause instanceof OAuth2AuthenticationException || cause instanceof OAuth2AuthorizationException
                || cause instanceof WebClientException || cause instanceof SSLException || isTransportFailure(cause)
        );
    }

    private static boolean isTransportFailure(Throwable cause) {
        return cause instanceof TimeoutException || cause instanceof SocketTimeoutException
            || cause instanceof ConnectException || cause instanceof UnknownHostException;
    }

    private static List<Throwable> causes(Throwable exception) {
        var visited = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
        var causes = new ArrayList<Throwable>();
        for (Throwable cause = exception; cause != null && visited.add(cause); cause = cause.getCause()) {
            causes.add(cause);
        }
        return causes;
    }

    private Failure classify(Throwable exception) {
        var causes = causes(exception);
        String errorCode = null;
        for (Throwable cause : causes) {
            if (cause instanceof SSLException) {
                return Failure.PROVIDER_ERROR;
            }
            if (cause instanceof WebClientResponseException response && response.getStatusCode().is5xxServerError()) {
                var status = response.getStatusCode().value();
                return status == HttpStatus.SC_SERVICE_UNAVAILABLE || status == HttpStatus.SC_GATEWAY_TIMEOUT ? Failure.PROVIDER_UNAVAILABLE : Failure.PROVIDER_ERROR;
            }
            if (isTransportFailure(cause)) {
                return Failure.PROVIDER_UNAVAILABLE;
            }
            if (errorCode == null) {
                if (cause instanceof OAuth2AuthenticationException oauth2) {
                    errorCode = oauth2.getError().getErrorCode();
                } else if (cause instanceof OAuth2AuthorizationException oauth2) {
                    errorCode = oauth2.getError().getErrorCode();
                }
            }
        }
        var failure = errorCode == null ? Failure.UNKNOWN : fromErrorCode(errorCode);
        // Transport exceptions may wrap TLS errors, so inspect their causes before applying this fallback.
        // Spring reports a failed token or user info request as an invalid provider response, keep the transport cause instead.
        var providerOrUnknown = failure == Failure.UNKNOWN || failure == Failure.PROVIDER_ERROR;
        if (providerOrUnknown && causes.stream().anyMatch(WebClientRequestException.class::isInstance)) {
            return Failure.PROVIDER_UNAVAILABLE;
        }
        return failure;
    }

    private static Failure fromErrorCode(String errorCode) {
        return switch (errorCode) {
            case "access_denied", "login_required", "interaction_required", "consent_required",
                 "account_selection_required", "invalid_grant" -> Failure.REJECTED;
            case "invalid_state_parameter", "authorization_request_not_found", "invalid_nonce",
                 "client_registration_not_found" -> Failure.INVALID_LOGIN;
            case "invalid_client", "unauthorized_client", "unsupported_grant_type", "invalid_scope",
                 "missing_user_info_uri", "missing_user_name_attribute", "invalid_id_token" -> Failure.CONFIGURATION;
            case "temporarily_unavailable" -> Failure.PROVIDER_UNAVAILABLE;
            case "server_error", "invalid_token_response", "invalid_user_info_response" -> Failure.PROVIDER_ERROR;
            // invalid_request and provider-specific codes do not reliably identify the source of the problem.
            default -> Failure.UNKNOWN;
        };
    }

    @RequiredArgsConstructor
    private enum Failure {
        REJECTED(401, "oidcLoginRejected"),
        INVALID_LOGIN(400, "oidcInvalidLogin"),
        CONFIGURATION(500, "oidcConfigurationError"),
        PROVIDER_UNAVAILABLE(503, "oidcProviderUnavailable"),
        PROVIDER_ERROR(502, "oidcProviderUnavailable"),
        UNKNOWN(500, "oidcAuthenticationFailed");

        private final int status;
        private final String message;

        private String getMessageKey() {
            return "org.zowe.apiml.gateway." + message;
        }
    }
}
