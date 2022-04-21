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
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHeader;
import org.springframework.stereotype.Component;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.gateway.security.service.schema.source.JwtAuthSource;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.util.CookieUtil;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.zowe.apiml.gateway.security.service.schema.X509Scheme.AUTH_FAIL_HEADER;

/**
 * This bean support PassTicket. Bean is responsible for getting PassTicket from
 * SAF and generating new authentication header in request.
 */
@Component
public class HttpBasicPassTicketScheme implements IAuthenticationScheme {

    private final PassTicketService passTicketService;
    private final AuthSourceService authSourceService;
    private final AuthConfigurationProperties authConfigurationProperties;
    private final MessageService messageService;
    private final String cookieName;

    public HttpBasicPassTicketScheme(
        PassTicketService passTicketService,
        AuthSourceService authSourceService,
        AuthConfigurationProperties authConfigurationProperties,
        MessageService messageService
    ) {
        this.passTicketService = passTicketService;
        this.authSourceService = authSourceService;
        this.authConfigurationProperties = authConfigurationProperties;
        cookieName = authConfigurationProperties.getCookieProperties().getCookieName();
        this.messageService = messageService;
    }

    @Override
    public AuthenticationScheme getScheme() {
        return AuthenticationScheme.HTTP_BASIC_PASSTICKET;
    }

    @Override
    public AuthenticationCommand createCommand(Authentication authentication, AuthSource authSource) {
        final RequestContext context = RequestContext.getCurrentContext();
        // Check for error in context to use it in header "X-Zowe-Auth-Failure"
        if (context.containsKey(AUTH_FAIL_HEADER)) {
            String errorHeaderValue = context.get(AUTH_FAIL_HEADER).toString();
            // this command should expire immediately after creation because it is build based on missing/incorrect authentication
            return new PassTicketCommand(null, cookieName, System.currentTimeMillis(), errorHeaderValue);
        }

        final long before = System.currentTimeMillis();


        AuthSource.Parsed parsedAuthSource;
        String error;
        try {
            parsedAuthSource = authSourceService.parse(authSource);
        } catch (TokenNotValidException e) {
            error = this.messageService.createMessage("org.zowe.apiml.gateway.security.invalidToken").mapToLogMessage();
            return new PassTicketCommand(null, cookieName, null, error);
        }
//        } catch (TokenExpireException e) {
//            error = this.messageService.createMessage("org.zowe.apiml.gateway.security.expiredToken").mapToLogMessage();
//            return new PassTicketCommand(null, cookieName, null, error);
//        }

        if (authSource == null || authSource.getRawSource() == null) {
            error = this.messageService.createMessage("org.zowe.apiml.gateway.security.schema.missingAuthentication").mapToLogMessage();
            return new PassTicketCommand(null, cookieName, null, error);
        }
        else if (parsedAuthSource == null) { // invalid authSource - can be due to
            error = this.messageService.createMessage("org.zowe.apiml.gateway.security.scheme.x509ParsingError", "Cannot parse provided authentication source").mapToLogMessage();
            return new PassTicketCommand(null, cookieName, null, error);
        }
        else if (parsedAuthSource.getUserId() == null) {
            error = this.messageService.createMessage("org.zowe.apiml.gateway.security.schema.x509.mappingFailed").mapToLogMessage();
            return new PassTicketCommand(null, cookieName, null, error);
        }

        final String applId = authentication.getApplid();
        final String userId = parsedAuthSource.getUserId();
        String passTicket;
        try {
            passTicket = passTicketService.generate(userId, applId);
        } catch (IRRPassTicketGenerationException e) {
            error = String.format("Could not generate PassTicket for user ID %s and APPLID %s", userId, applId);
            return new PassTicketCommand(null, cookieName, null, error);
        }
        final String encoded = Base64.getEncoder()
            .encodeToString((userId + ":" + passTicket).getBytes(StandardCharsets.UTF_8));
        final String value = "Basic " + encoded;

        final long expiredAt = Math.min(before + authConfigurationProperties.getPassTicket().getTimeout() * 1000,
            parsedAuthSource.getExpiration().getTime());

        return new PassTicketCommand(value, cookieName, expiredAt, null);
    }

    @Override
    public Optional<AuthSource> getAuthSource() {
        return authSourceService.getAuthSourceFromRequest();
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class PassTicketCommand extends AuthenticationCommand {

        private static final long serialVersionUID = 3941300386857998443L;

        private static final String COOKIE_HEADER = "cookie";

        private final String authorizationValue;
        private final String cookieName;
        private final Long expireAt;
        private final String errorValue;

        @Override
        public void apply(InstanceInfo instanceInfo) {
            final RequestContext context = RequestContext.getCurrentContext();
            if (authorizationValue != null) {
                context.addZuulRequestHeader(HttpHeaders.AUTHORIZATION, authorizationValue);
                context.addZuulRequestHeader(COOKIE_HEADER,
                    CookieUtil.removeCookie(
                        context.getZuulRequestHeaders().get(COOKIE_HEADER),
                        cookieName
                    )
                );
            }
            else {
                JwtCommand.setErrorHeader(context, errorValue);
            }
        }

        @Override
        public void applyToRequest(HttpRequest request) {
            if (authorizationValue != null) {
                request.setHeader(
                    new BasicHeader(HttpHeaders.AUTHORIZATION, authorizationValue)
                );
                Header header = request.getFirstHeader(COOKIE_HEADER);
                if (header != null) {
                    request.setHeader(COOKIE_HEADER,
                        CookieUtil.removeCookie(
                            header.getValue(),
                            cookieName
                        )
                    );
                }
            }
            else {
                request.addHeader(AUTH_FAIL_HEADER, errorValue);
            }
        }

        @Override
        public boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }

        @Override
        public boolean isRequiredValidSource() {
            return true;
        }

    }
}
