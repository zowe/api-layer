/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpUpgradeHandler;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.gateway.filters.AbstractAuthSchemeFactory.AuthorizationResponse;
import org.zowe.apiml.gateway.filters.ErrorHeaders;
import org.zowe.apiml.gateway.filters.RequestCredentials;
import org.zowe.apiml.gateway.filters.ZaasInternalErrorException;
import org.zowe.apiml.gateway.filters.ZaasSchemeTransform;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.passticket.ApplicationNameNotProvidedException;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.product.opentelemetry.OtelRequestContext;
import org.zowe.apiml.ticket.TicketResponse;
import org.zowe.apiml.zaas.ZaasTokenResponse;
import org.zowe.apiml.zaas.security.service.TokenCreationService;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.zaas.security.service.schema.source.OIDCAuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.PATAuthSource;
import org.zowe.apiml.zaas.security.service.zosmf.ZosmfService;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Optional;

import static org.zowe.apiml.security.SecurityUtils.COOKIE_AUTH_NAME;
import static org.zowe.apiml.security.common.filter.CategorizeCertsFilter.ATTR_NAME_CLIENT_AUTH_X509_CERTIFICATE;

/**
 * {@code ZaasSchemeTransformApi} is the internal implementation of {@link ZaasSchemeTransform}
 * <p>
 * Unlike {@code ZaasSchemeTransformRest}, which makes HTTP requests to the ZAAS service,
 * this implementation directly invokes service layer components within the same application context.
 * </p>
 *
 * <p>
 * This class provides support for authentication schemes like:
 * <ul>
 *     <li>PassTicket generation</li>
 *     <li>SAF Identity Token generation</li>
 *     <li>z/OSMF token exchange</li>
 *     <li>Zowe JWT generation</li>
 * </ul>
 *
 * <p>
 * This bean is only active when {@code modulithConfig} is present in the Spring context.
 * </p>
 *
 * @see ZaasSchemeTransform
 * @see org.zowe.apiml.gateway.filters.ZaasSchemeTransformRest
 */

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnBean(name = "modulithConfig")
public class ZaasSchemeTransformApi implements ZaasSchemeTransform {

    private static final ClientResponse.Headers EMPTY_HEADERS = new ErrorHeaders();

    private final AuthSourceService authSourceService;
    private final PassTicketService passTicketService;
    private final ZosmfService zosmfService;
    private final TokenCreationService tokenCreationService;
    private final MessageService messageService;

    @Value("${apiml.service.apimlId:apiml}")
    private String currentApimlId;

    private ErrorHeaders createErrorMessage(String errorMessage) {
        return new ErrorHeaders(errorMessage);
    }

    private <R> Mono<AuthorizationResponse<R>> createInvalidAuthenticationErrorMessage() {
        var messageKey = "org.zowe.apiml.common.unauthorized";
        var logMessage = messageService.createMessage(messageKey).mapToLogMessage();
        var headers = new ErrorHeaders(logMessage);
        return Mono.just(new AuthorizationResponse<>(headers, null));
    }

    private AuthorizationResponse<String> createMissingAuthenticationErrorMessage() {
        var messageKey = "org.zowe.apiml.zaas.security.schema.missingAuthentication";
        var logMessage = messageService.createMessage(messageKey).mapToLogMessage();
        return new AuthorizationResponse<>(createErrorMessage(logMessage), InsufficientAuthenticationException.class.getName());
    }

    private <R> Mono<AuthorizationResponse<R>> createAuthorizationResponse(ErrorHeaders headers, R response) {
        return Mono.just(new AuthorizationResponse<>(headers, response));
    }

    private <R> Mono<AuthorizationResponse<R>> handleMissingOrInvalidAuth(OtelRequestContext context) {
        var response = createMissingAuthenticationErrorMessage();
        context.authErrorType(response.getBody());
        return createAuthorizationResponse((ErrorHeaders) response.getHeaders(), null);
    }

    private <R> Mono<AuthorizationResponse<R>> handleMissingApplicationName(String serviceId, OtelRequestContext context) {
        context.authErrorType(ApplicationNameNotProvidedException.class.getName());
        log.debug("Service '{}' is missing APPLID set", serviceId);
        return createAuthorizationResponse(createErrorMessage("ApplicationName not provided."),null);
    }

    @Override
    public Mono<AuthorizationResponse<TicketResponse>> passticket(RequestCredentials requestCredentials, ServerWebExchange exchange) {
        var applicationName = requestCredentials.getApplId();
        var otelRequestContext = OtelRequestContext.of(exchange);
        if (StringUtils.isBlank(applicationName)) {
            return handleMissingApplicationName(requestCredentials.getServiceId(), otelRequestContext);
        }

        try {
            var request = new RequestCredentialsHttpServletRequestAdapter(requestCredentials);
            Optional<AuthSource> authSource = authSourceService.getAuthSourceFromRequest(request);
            if (authSource.isEmpty()) {
                return handleMissingOrInvalidAuth(otelRequestContext);
            }
            updateServiceId(authSource, request);
            if (!authSourceService.isValid(authSource.get())) {
                return handleMissingOrInvalidAuth(otelRequestContext);
            }
            var authSourceParsed = authSourceService.parse(authSource.get());

            var ticket = passTicketService.generate(authSourceParsed.getUserId(), applicationName);
            var response = TicketResponse.builder()
                .token("")
                .userId(authSourceParsed.getUserId())
                .applicationName(applicationName)
                .ticket(ticket)
                .distributedIds(authSource.filter(OIDCAuthSource.class::isInstance)
                    .map(OIDCAuthSource.class::cast)
                    .map(OIDCAuthSource::getDistributedId)
                    .orElse(null))
                .authSourceType(authSource.map(AuthSource::getType).map(Enum::name).orElse(null))
                .build();

            return Mono.just(new AuthorizationResponse<>(EMPTY_HEADERS, response));
        } catch (IRRPassTicketGenerationException e) {
            log.debug("Cannot generate ticket", e);
            return Mono.error(new ZaasInternalErrorException(currentApimlId, e.getMessage()));
        } catch (Exception e) {
            log.debug("Token has expired", e);
            otelRequestContext.authErrorType(e.getClass().getName());
            return createInvalidAuthenticationErrorMessage();
        }
    }

    private void updateServiceId(Optional<AuthSource> authSource, RequestCredentialsHttpServletRequestAdapter request) {
        authSource
            .filter(PATAuthSource.class::isInstance)
            .map(PATAuthSource.class::cast)
            .filter(as -> StringUtils.isBlank(as.getDefaultServiceId()))
            .ifPresent(as -> as.setDefaultServiceId(request.getServiceId()));
    }

    @Override
    public Mono<AuthorizationResponse<ZaasTokenResponse>> safIdt(RequestCredentials requestCredentials, ServerWebExchange exchange) {
        var applicationName = requestCredentials.getApplId();
        var otelRequestContext = OtelRequestContext.of(exchange);
        if (StringUtils.isBlank(applicationName)) {
            return handleMissingApplicationName(requestCredentials.getServiceId(), otelRequestContext);
        }

        try {
            var request = new RequestCredentialsHttpServletRequestAdapter(requestCredentials);
            Optional<AuthSource> authSource = authSourceService.getAuthSourceFromRequest(request);
            if (authSource.isEmpty()) {
                return handleMissingOrInvalidAuth(otelRequestContext);
            }
            updateServiceId(authSource, request);
            if (!authSourceService.isValid(authSource.get())) {
                return handleMissingOrInvalidAuth(otelRequestContext);
            }
            var authSourceParsed = authSourceService.parse(authSource.get());

            String safIdToken = tokenCreationService.createSafIdTokenWithoutCredentials(authSourceParsed.getUserId(), applicationName);
            var response = ZaasTokenResponse.builder()
                .headerName(ApimlConstants.SAF_TOKEN_HEADER)
                .token(safIdToken)
                .userId(authSourceParsed.getUserId())
                .distributedIds(authSource.filter(OIDCAuthSource.class::isInstance)
                    .map(OIDCAuthSource.class::cast)
                    .map(OIDCAuthSource::getDistributedId)
                    .orElse(null)
                )
                .authSourceType(authSource.map(AuthSource::getType).map(Enum::name).orElse(null))
                .build();
            return Mono.just(new AuthorizationResponse<>(EMPTY_HEADERS, response));
        } catch (Exception e) {
            log.debug("Cannot generate SAF IDT", e);
            otelRequestContext.authErrorType(e.getClass().getName());
            return createAuthorizationResponse(createErrorMessage(e.getMessage()), null);
        }
    }

    @Override
    public Mono<AuthorizationResponse<ZaasTokenResponse>> zosmf(RequestCredentials requestCredentials, ServerWebExchange exchange) {
        var otelRequestContext = OtelRequestContext.of(exchange);
        try {
            var request = new RequestCredentialsHttpServletRequestAdapter(requestCredentials);
            Optional<AuthSource> authSource = authSourceService.getAuthSourceFromRequest(request);
            if (authSource.isEmpty()) {
                return handleMissingOrInvalidAuth(otelRequestContext);
            }
            updateServiceId(authSource, request);
            if (!authSourceService.isValid(authSource.get())) {
                return handleMissingOrInvalidAuth(otelRequestContext);
            }
            var authSourceParsed = authSourceService.parse(authSource.get());

            var response = zosmfService.exchangeAuthenticationForZosmfToken(authSource.get().getRawSource().toString(), authSourceParsed);
            response.setAuthSourceType(authSource.map(AuthSource::getType).map(Enum::name).orElse(null));

            authSource.filter(OIDCAuthSource.class::isInstance)
                .map(OIDCAuthSource.class::cast)
                .map(OIDCAuthSource::getDistributedId)
                .ifPresent(response::setDistributedIds);
            authSource.map(AuthSource::getType).map(Enum::name).ifPresent(response::setAuthSourceType);

            return Mono.just(new AuthorizationResponse<>(EMPTY_HEADERS, response));
        } catch (Exception e) {
            log.debug("Cannot obtain z/OSMF token", e);
            otelRequestContext.authErrorType(e.getClass().getName());
            return createAuthorizationResponse(createErrorMessage(e.getMessage()), null);
        }
    }

    @Override
    public Mono<AuthorizationResponse<ZaasTokenResponse>> zoweJwt(RequestCredentials requestCredentials, ServerWebExchange exchange) {
        var otelRequestContext = OtelRequestContext.of(exchange);
        try {
            var request = new RequestCredentialsHttpServletRequestAdapter(requestCredentials);
            Optional<AuthSource> authSource = authSourceService.getAuthSourceFromRequest(request);
            if (authSource.isEmpty()) {
                return handleMissingOrInvalidAuth(otelRequestContext);
            }
            updateServiceId(authSource, request);
            if (!authSourceService.isValid(authSource.get())) {
                return handleMissingOrInvalidAuth(otelRequestContext);
            }
            var authSourceParsed = authSourceService.parse(authSource.get());
            var token = authSourceService.getJWT(authSource.get());
            var response = ZaasTokenResponse.builder().cookieName(COOKIE_AUTH_NAME)
                .token(token)
                .userId(authSourceParsed.getUserId())
                .distributedIds(authSource.filter(OIDCAuthSource.class::isInstance)
                    .map(OIDCAuthSource.class::cast)
                    .map(OIDCAuthSource::getDistributedId)
                    .orElse(null)
                )
                .authSourceType(authSource.map(AuthSource::getType).map(Enum::name).orElse(null))
                .build();
            return Mono.just(new AuthorizationResponse<>(EMPTY_HEADERS, response));
        } catch (Exception e) {
            log.debug("Cannot obtain Zowe JWT token", e);
            otelRequestContext.authErrorType(e.getClass().getName());
            return createInvalidAuthenticationErrorMessage();
        }
    }

    @RequiredArgsConstructor
    private static class RequestCredentialsHttpServletRequestAdapter implements HttpServletRequest {

        private final RequestCredentials requestCredentials;

        @Delegate(excludes = Exclude.class)
        private HttpServletRequest request;

        public String getServiceId() {
            return requestCredentials.getServiceId();
        }

        @Override
        public Cookie[] getCookies() {
            return Optional.ofNullable(requestCredentials.getCookies())
                .orElse(Collections.emptyMap())
                .entrySet().stream()
                .map(entry -> {
                    var cookie = new Cookie(entry.getKey(), entry.getValue());
                    cookie.setSecure(true);
                    cookie.setHttpOnly(true);
                    return cookie;
                })
                .toArray(Cookie[]::new);
        }

        @Override
        public String getHeader(String name) {
            return Optional.ofNullable(requestCredentials.getHeaders())
                .map(h -> h.get(StringUtils.lowerCase(name)))
                .map(a -> a.length > 0 ? a[0] : null)
                .orElse(null);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return Collections.enumeration(
                Optional.ofNullable(requestCredentials.getHeaders())
                    .map(h -> h.get(name))
                    .map(Arrays::asList)
                    .orElse(Collections.emptyList())
            );
        }

        @Override
        public Object getAttribute(String name) {
            if (ATTR_NAME_CLIENT_AUTH_X509_CERTIFICATE.equals(name)) {
                try {
                    var certBase64 = requestCredentials.getX509Certificate();
                    if (StringUtils.isBlank(certBase64)) return null;

                    byte[] certBytes = Base64.getDecoder().decode(certBase64);
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes));
                    return new X509Certificate[]{cert};
                } catch (Exception e) {
                    log.debug("Invalid certificate format in RequestCredentials", e);
                    return null;
                }
            }
            return null;
        }

        @Override
        public String getRequestURI() {
            return requestCredentials.getRequestURI();
        }

        @Override
        public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) {
            throw new UnsupportedOperationException();
        }

        interface Exclude {
            Enumeration<String> getHeaders(String name);
        }

    }

}
