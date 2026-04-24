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

import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.ExpiredJWTException;
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
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.gateway.filters.*;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.passticket.ApplicationNameNotProvidedException;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
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
import java.text.ParseException;
import java.util.*;

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

    private ErrorHeaders createInvalidAuthenticationErrorMessage() {
        String messageKey = "org.zowe.apiml.common.unauthorized";
        String logMessage = messageService.createMessage(messageKey).mapToLogMessage();
        return new ErrorHeaders(logMessage);
    }

    private AbstractAuthSchemeFactory.AuthorizationResponse<String> createMissingAuthenticationErrorMessage() {
        String messageKey = "org.zowe.apiml.zaas.security.schema.missingAuthentication";
        String logMessage = messageService.createMessage(messageKey).mapToLogMessage();
        return new AbstractAuthSchemeFactory.AuthorizationResponse<>(createErrorMessage(logMessage), InsufficientAuthenticationException.class.getName());
    }

    private <R> Mono<AbstractAuthSchemeFactory.AuthorizationResponse<R>> createAuthorizationResponse(ErrorHeaders headers, R response) {
        return Mono.just(new AbstractAuthSchemeFactory.AuthorizationResponse<>(headers, response));
    }

    @Override
    public Mono<AbstractAuthSchemeFactory.AuthorizationResponse<TicketResponse>> passticket(RequestCredentials requestCredentials) {
        var applicationName = requestCredentials.getApplId();
        if (StringUtils.isBlank(applicationName)) {
            // TODO update errorType when passticket ApplId is missing
            return createAuthorizationResponse(createErrorMessage("ApplicationName not provided."), TicketResponse.builder().errorType(ApplicationNameNotProvidedException.class.getName()).build());
        }

        try {
            var request = new RequestCredentialsHttpServletRequestAdapter(requestCredentials);
            Optional<AuthSource> authSource = authSourceService.getAuthSourceFromRequest(request);
            var missingAuthenticationErrorResponse = createMissingAuthenticationErrorMessage();
            if (authSource.isEmpty()) {
                return createAuthorizationResponse((ErrorHeaders) missingAuthenticationErrorResponse.getHeaders(), TicketResponse.builder().errorType(missingAuthenticationErrorResponse.getBody()).build());
            }
            updateServiceId(authSource, request);
            if (!authSourceService.isValid(authSource.get())) {
                return createAuthorizationResponse((ErrorHeaders) missingAuthenticationErrorResponse.getHeaders(), TicketResponse.builder().errorType(missingAuthenticationErrorResponse.getBody()).build());
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

            return Mono.just(new AbstractAuthSchemeFactory.AuthorizationResponse<>(EMPTY_HEADERS, response));
        } catch (IRRPassTicketGenerationException e) {
            log.debug("Cannot generate ticket", e);
            return Mono.error(new ZaasInternalErrorException(currentApimlId, e.getMessage()));
        } catch (Exception e) {
            log.debug("Token has expired", e);
            return createAuthorizationResponse(createInvalidAuthenticationErrorMessage(), TicketResponse.builder().errorType(e.getClass().getName()).build());
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
    public Mono<AbstractAuthSchemeFactory.AuthorizationResponse<ZaasTokenResponse>> safIdt(RequestCredentials requestCredentials) {
        var applicationName = requestCredentials.getApplId();
        if (StringUtils.isBlank(applicationName)) {
            // TODO update errorType when ApplId is missing
            return createAuthorizationResponse(createErrorMessage("ApplicationName not provided."), ZaasTokenResponse.builder().errorType("").build());
        }

        try {
            var request = new RequestCredentialsHttpServletRequestAdapter(requestCredentials);
            Optional<AuthSource> authSource = authSourceService.getAuthSourceFromRequest(request);
            var missingAuthenticationErrorResponse = createMissingAuthenticationErrorMessage();
            if (authSource.isEmpty()) {
                return createAuthorizationResponse((ErrorHeaders) missingAuthenticationErrorResponse.getHeaders(), ZaasTokenResponse.builder().errorType(missingAuthenticationErrorResponse.getBody()).build());
            }
            updateServiceId(authSource, request);
            if (!authSourceService.isValid(authSource.get())) {
                return createAuthorizationResponse((ErrorHeaders) missingAuthenticationErrorResponse.getHeaders(), ZaasTokenResponse.builder().errorType(missingAuthenticationErrorResponse.getBody()).build());
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
            return Mono.just(new AbstractAuthSchemeFactory.AuthorizationResponse<>(EMPTY_HEADERS, response));
        } catch (Exception e) {
            log.debug("Cannot generate SAF IDT", e);
            return createAuthorizationResponse(createErrorMessage(e.getMessage()), ZaasTokenResponse.builder().errorType(e.getClass().getName()).build());
        }
    }

    @Override
    public Mono<AbstractAuthSchemeFactory.AuthorizationResponse<ZaasTokenResponse>> zosmf(RequestCredentials requestCredentials) {
        try {
            var request = new RequestCredentialsHttpServletRequestAdapter(requestCredentials);
            Optional<AuthSource> authSource = authSourceService.getAuthSourceFromRequest(request);
            var missingAuthenticationErrorResponse = createMissingAuthenticationErrorMessage();
            if (authSource.isEmpty()) {
                return createAuthorizationResponse((ErrorHeaders) missingAuthenticationErrorResponse.getHeaders(), ZaasTokenResponse.builder().errorType(missingAuthenticationErrorResponse.getBody()).build());
            }
            updateServiceId(authSource, request);
            if (!authSourceService.isValid(authSource.get())) {
                return createAuthorizationResponse((ErrorHeaders) missingAuthenticationErrorResponse.getHeaders(), ZaasTokenResponse.builder().errorType(missingAuthenticationErrorResponse.getBody()).build());
            }
            var authSourceParsed = authSourceService.parse(authSource.get());

            var response = zosmfService.exchangeAuthenticationForZosmfToken(authSource.get().getRawSource().toString(), authSourceParsed);

            authSource.filter(OIDCAuthSource.class::isInstance)
                .map(OIDCAuthSource.class::cast)
                .map(OIDCAuthSource::getDistributedId)
                .ifPresent(response::setDistributedIds);
            authSource.map(AuthSource::getType).map(Enum::name).ifPresent(response::setAuthSourceType);

            return Mono.just(new AbstractAuthSchemeFactory.AuthorizationResponse<>(EMPTY_HEADERS, response));
        } catch (Exception e) {
            log.debug("Cannot obtain z/OSMF token", e);
            return createAuthorizationResponse(createErrorMessage(e.getMessage()), ZaasTokenResponse.builder().errorType(e.getClass().getName()).build());
        }
    }

    @Override
    public Mono<AbstractAuthSchemeFactory.AuthorizationResponse<ZaasTokenResponse>> zoweJwt(RequestCredentials requestCredentials) {
        var zaasTokenResponseBuilder = ZaasTokenResponse.builder();
        try {
            var request = new RequestCredentialsHttpServletRequestAdapter(requestCredentials);
            Optional<AuthSource> authSource = authSourceService.getAuthSourceFromRequest(request);
            var missingAuthenticationErrorResponse = createMissingAuthenticationErrorMessage();
            if (authSource.isEmpty()) {
                return createAuthorizationResponse((ErrorHeaders) missingAuthenticationErrorResponse.getHeaders(), zaasTokenResponseBuilder.errorType(missingAuthenticationErrorResponse.getBody()).build());
            }
            updateServiceId(authSource, request);
            if (!authSourceService.isValid(authSource.get())) {
                return createAuthorizationResponse((ErrorHeaders) missingAuthenticationErrorResponse.getHeaders(), zaasTokenResponseBuilder.errorType(missingAuthenticationErrorResponse.getBody()).build());
            }
            var authSourceParsed = authSourceService.parse(authSource.get());
            var token = authSourceService.getJWT(authSource.get());
            var response = zaasTokenResponseBuilder.cookieName(COOKIE_AUTH_NAME)
                .token(token)
                .userId(authSourceParsed.getUserId())
                .distributedIds(authSource.filter(OIDCAuthSource.class::isInstance)
                    .map(OIDCAuthSource.class::cast)
                    .map(OIDCAuthSource::getDistributedId)
                    .orElse(null)
                )
                .authSourceType(authSource.map(AuthSource::getType).map(Enum::name).orElse(null))
                .build();
            return Mono.just(new AbstractAuthSchemeFactory.AuthorizationResponse<>(EMPTY_HEADERS, response));
        } catch (Exception e) {
            log.debug("Cannot obtain Zowe JWT token", e);
            if (e.getCause() instanceof BadJWTException || e.getCause() instanceof ParseException || e.getCause() instanceof ExpiredJWTException) {
                zaasTokenResponseBuilder.authSourceType(AuthSource.AuthSourceType.JWT.name());
            }
            return createAuthorizationResponse(createInvalidAuthenticationErrorMessage(), zaasTokenResponseBuilder.errorType(e.getClass().getName()).build());

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
