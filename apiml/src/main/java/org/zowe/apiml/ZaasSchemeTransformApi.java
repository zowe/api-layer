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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpUpgradeHandler;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.gateway.filters.AbstractAuthSchemeFactory;
import org.zowe.apiml.gateway.filters.ErrorHeaders;
import org.zowe.apiml.gateway.filters.RequestCredentials;
import org.zowe.apiml.gateway.filters.ZaasInternalErrorException;
import org.zowe.apiml.gateway.filters.ZaasSchemeTransform;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.ticket.TicketResponse;
import org.zowe.apiml.zaas.ZaasTokenResponse;
import org.zowe.apiml.zaas.security.service.TokenCreationService;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.zaas.security.service.schema.source.PATAuthSource;
import org.zowe.apiml.zaas.security.service.zosmf.ZosmfService;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
 * </p>
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

    private <R> Mono<AbstractAuthSchemeFactory.AuthorizationResponse<R>> createErrorMessage(String errorMessage) {
        var headers = new ErrorHeaders(errorMessage);
        return Mono.just(new AbstractAuthSchemeFactory.AuthorizationResponse<>(headers, null));
    }

    private <R> Mono<AbstractAuthSchemeFactory.AuthorizationResponse<R>> createInvalidAuthenticationErrorMessage() {
        String messageKey = "org.zowe.apiml.common.unauthorized";
        String logMessage = messageService.createMessage(messageKey).mapToLogMessage();
        var headers = new ErrorHeaders(logMessage);
        return Mono.just(new AbstractAuthSchemeFactory.AuthorizationResponse<>(headers, null));
    }

    private <R> Mono<AbstractAuthSchemeFactory.AuthorizationResponse<R>> createMissingAuthenticationErrorMessage() {
        String messageKey = "org.zowe.apiml.zaas.security.schema.missingAuthentication";
        String logMessage = messageService.createMessage(messageKey).mapToLogMessage();
        var headers = new ErrorHeaders(logMessage);
        return Mono.just(new AbstractAuthSchemeFactory.AuthorizationResponse<>(headers, null));
    }

    @Override
    public Mono<AbstractAuthSchemeFactory.AuthorizationResponse<TicketResponse>> passticket(RequestCredentials requestCredentials) {
        var applicationName = requestCredentials.getApplId();
        if (StringUtils.isBlank(applicationName)) {
            return createErrorMessage("ApplicationName not provided.");
        }

        try {
            var request = new RequestCredentialsHttpServletRequestAdapter(requestCredentials);
            Optional<AuthSource> authSource = authSourceService.getAuthSourceFromRequest(request);
            if (authSource.isEmpty()) {
                return createMissingAuthenticationErrorMessage();
            }
            updateServiceId(authSource, request);
            if (!authSourceService.isValid(authSource.get())) {
                return createInvalidAuthenticationErrorMessage();
            }
            var authSourceParsed = authSourceService.parse(authSource.get());

            var ticket = passTicketService.generate(authSourceParsed.getUserId(), applicationName);
            var response = new TicketResponse("", authSourceParsed.getUserId(), applicationName, ticket);
            return Mono.just(new AbstractAuthSchemeFactory.AuthorizationResponse<>(EMPTY_HEADERS, response));
        } catch (IRRPassTicketGenerationException e) {
            log.debug("Cannot generate ticket", e);
            return Mono.error(new ZaasInternalErrorException(currentApimlId, e.getMessage()));
        } catch (Exception e) {
            log.debug("Token has expired", e);
            return createErrorMessage(e.getMessage());
        }
    }

    private void updateServiceId(Optional<AuthSource> authSource, RequestCredentialsHttpServletRequestAdapter request) {
        authSource
            .filter(PATAuthSource.class::isInstance)
            .map(PATAuthSource.class::cast)
            .filter(as -> as.getDefaultServiceId() == null)
            .ifPresent(as -> as.setDefaultServiceId(request.getServiceId()));
    }

    @Override
    public Mono<AbstractAuthSchemeFactory.AuthorizationResponse<ZaasTokenResponse>> safIdt(RequestCredentials requestCredentials) {
        var applicationName = requestCredentials.getApplId();
        if (StringUtils.isBlank(applicationName)) {
            return createErrorMessage("ApplicationName not provided.");
        }

        try {
            var request = new RequestCredentialsHttpServletRequestAdapter(requestCredentials);
            Optional<AuthSource> authSource = authSourceService.getAuthSourceFromRequest(request);
            if (authSource.isEmpty()) {
                return createMissingAuthenticationErrorMessage();
            }
            updateServiceId(authSource, request);
            if (!authSourceService.isValid(authSource.get())) {
                return createInvalidAuthenticationErrorMessage();
            }
            var authSourceParsed = authSourceService.parse(authSource.get());

            String safIdToken = tokenCreationService.createSafIdTokenWithoutCredentials(authSourceParsed.getUserId(), applicationName);
            var response = ZaasTokenResponse.builder().headerName(ApimlConstants.SAF_TOKEN_HEADER).token(safIdToken).build();
            return Mono.just(new AbstractAuthSchemeFactory.AuthorizationResponse<>(EMPTY_HEADERS, response));
        } catch (Exception e) {
            log.debug("Cannot generate SAF IDT", e);
            return createErrorMessage(e.getMessage());
        }
    }

    @Override
    public Mono<AbstractAuthSchemeFactory.AuthorizationResponse<ZaasTokenResponse>> zosmf(RequestCredentials requestCredentials) {
        try {
            var request = new RequestCredentialsHttpServletRequestAdapter(requestCredentials);
            Optional<AuthSource> authSource = authSourceService.getAuthSourceFromRequest(request);
            if (authSource.isEmpty()) {
                return createMissingAuthenticationErrorMessage();
            }
            updateServiceId(authSource, request);
            if (!authSourceService.isValid(authSource.get())) {
                return createInvalidAuthenticationErrorMessage();
            }
            var authSourceParsed = authSourceService.parse(authSource.get());

            var response = zosmfService.exchangeAuthenticationForZosmfToken(authSource.get().getRawSource().toString(), authSourceParsed);
            return Mono.just(new AbstractAuthSchemeFactory.AuthorizationResponse<>(EMPTY_HEADERS, response));
        } catch (Exception e) {
            log.debug("Cannot obtain z/OSMF token", e);
            return createErrorMessage(e.getMessage());
        }
    }

    @Override
    public Mono<AbstractAuthSchemeFactory.AuthorizationResponse<ZaasTokenResponse>> zoweJwt(RequestCredentials requestCredentials) {
        try {
            var request = new RequestCredentialsHttpServletRequestAdapter(requestCredentials);
            Optional<AuthSource> authSource = authSourceService.getAuthSourceFromRequest(request);
            if (authSource.isEmpty()) {
                return createMissingAuthenticationErrorMessage();
            }
            updateServiceId(authSource, request);
            if (!authSourceService.isValid(authSource.get())) {
                return createInvalidAuthenticationErrorMessage();
            }
            var token = authSourceService.getJWT(authSource.get());
            var response = ZaasTokenResponse.builder().cookieName(COOKIE_AUTH_NAME).token(token).build();
            return Mono.just(new AbstractAuthSchemeFactory.AuthorizationResponse<>(EMPTY_HEADERS, response));
        } catch (Exception e) {
            log.debug("Cannot obtain Zowe JWT token", e);
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
        public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
            return request.upgrade(handlerClass);
        }

        interface Exclude {
            Enumeration<String> getHeaders(String name);
        }

    }

}
