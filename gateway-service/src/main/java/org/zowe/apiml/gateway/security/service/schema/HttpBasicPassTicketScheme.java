/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.service.schema;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.zuul.context.RequestContext;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSchemeException;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * This bean support PassTicket. Bean is responsible for getting PassTicket from
 * SAF and generating new authentication header in request.
 */
@Component
public class HttpBasicPassTicketScheme implements IAuthenticationScheme {
    @InjectApimlLogger
    private final ApimlLogger logger = ApimlLogger.empty();

    private final PassTicketService passTicketService;
    private final AuthSourceService authSourceService;
    private final String cookieName;
    private final String patCookieName;

    @org.springframework.beans.factory.annotation.Value("${apiml.security.auth.passticket.customUserHeader:}")
    private String customUserHeader;

    @org.springframework.beans.factory.annotation.Value("${apiml.security.auth.passticket.customAuthHeader:}")
    private String customPassTicketHeader;

    public HttpBasicPassTicketScheme(
        PassTicketService passTicketService,
        AuthSourceService authSourceService,
        AuthConfigurationProperties authConfigurationProperties
    ) {
        this.passTicketService = passTicketService;
        this.authSourceService = authSourceService;
        cookieName = authConfigurationProperties.getCookieProperties().getCookieName();
        patCookieName = authConfigurationProperties.getCookieProperties().getCookieNamePAT();
    }

    @Override
    public AuthenticationScheme getScheme() {
        return AuthenticationScheme.HTTP_BASIC_PASSTICKET;
    }

    @Override
    public AuthenticationCommand createCommand(Authentication authentication, AuthSource authSource) {

        if (authSource == null || authSource.getRawSource() == null) {
            throw new AuthSchemeException("org.zowe.apiml.gateway.security.schema.missingAuthentication");
        }

        AuthSource.Parsed parsedAuthSource;
        try {
            parsedAuthSource = authSourceService.parse(authSource);
            if (parsedAuthSource == null) {
                throw new IllegalStateException("Error occurred while parsing authentication source");
            }

            if (parsedAuthSource.getUserId() == null) {
                logger.log(MessageType.DEBUG, "It was not possible to map provided certificate to the mainframe identity.");
                throw new AuthSchemeException("org.zowe.apiml.gateway.security.schema.x509.mappingFailed");
            }
        } catch (TokenNotValidException e) {
            logger.log(MessageType.DEBUG, e.getLocalizedMessage());
            throw new AuthSchemeException("org.zowe.apiml.gateway.security.invalidToken");
        } catch (TokenExpireException e) {
            logger.log(MessageType.DEBUG, e.getLocalizedMessage());
            throw new AuthSchemeException("org.zowe.apiml.gateway.security.expiredToken");
        }

        final String applId = authentication.getApplid();
        final String userId = parsedAuthSource.getUserId();
        String passTicket;
        try {
            passTicket = passTicketService.generate(userId, applId);
        } catch (IRRPassTicketGenerationException e) {
            String error = String.format("Could not generate PassTicket for user ID %s and APPLID %s", userId, applId);
            logger.log(MessageType.DEBUG, error);
            throw new AuthSchemeException("org.zowe.apiml.security.ticket.generateFailed", error);
        }
        final String encoded = Base64.getEncoder()
            .encodeToString((userId + ":" + passTicket).getBytes(StandardCharsets.UTF_8));
        final String value = "Basic " + encoded;
//        passticket is valid only once, therefore this command needs to expire immediately and each call should generate new passticket
        long expiration = System.currentTimeMillis();
        return new PassTicketCommand(value, cookieName, patCookieName, expiration, customUserHeader, customPassTicketHeader, userId, passTicket);
    }

    @Override
    public Optional<AuthSource> getAuthSource() {
        return authSourceService.getAuthSourceFromRequest(RequestContext.getCurrentContext().getRequest());
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    @Slf4j
    public static class PassTicketCommand extends AuthenticationCommand {

        private static final long serialVersionUID = 3941300386857998443L;

        private static final String COOKIE_HEADER = "cookie";

        String authorizationValue;
        String cookieName;
        String patCookieName;
        Long expireAt;
        String customUserHeader;
        String customPassTicketHeader;
        String userId;
        String passTicket;

        @Override
        public void apply(InstanceInfo instanceInfo) {
            if (authorizationValue != null) {
                final RequestContext context = RequestContext.getCurrentContext();
                context.addZuulRequestHeader(HttpHeaders.AUTHORIZATION, authorizationValue);
                if (StringUtils.isNotEmpty(customUserHeader) && StringUtils.isNotEmpty(customPassTicketHeader)) {
                    log.debug("Adding HTTP request header {} to store the user ID", customUserHeader);
                    log.debug("Adding HTTP request header {} to store the passticket", customPassTicketHeader);
                    context.addZuulRequestHeader(customUserHeader, userId);
                    context.addZuulRequestHeader(customPassTicketHeader, passTicket);
                }
                String[] cookiesToBeRemoved = new String[]{cookieName,patCookieName};
                JwtCommand.removeCookie(context, cookiesToBeRemoved);
            }
        }

        @Override
        public boolean isExpired() {
            if (expireAt == null) return false;

            return System.currentTimeMillis() > expireAt;
        }

        @Override
        public boolean isRequiredValidSource() {
            return true;
        }

    }
}
