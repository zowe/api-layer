/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.service.schema;

import com.ca.apiml.security.common.auth.Authentication;
import com.ca.apiml.security.common.auth.AuthenticationScheme;
import com.ca.apiml.security.common.config.AuthConfigurationProperties;
import com.ca.mfaas.passticket.IRRPassTicketGenerationException;
import com.ca.mfaas.passticket.PassTicketService;
import com.ca.apiml.security.common.token.QueryResponse;
import com.ca.mfaas.gateway.security.service.AuthenticationException;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.zuul.context.RequestContext;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.apache.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * This bean support PassTicket. Bean is responsible for getting PassTicket from
 * SAF and generating new authentication header in request.
 */
@Component
@RequiredArgsConstructor
public class HttpBasicPassTicketScheme implements AbstractAuthenticationScheme {
    private final PassTicketService passTicketService;
    private final AuthConfigurationProperties authConfigurationProperties;

    @Override
    public AuthenticationScheme getScheme() {
        return AuthenticationScheme.HTTP_BASIC_PASSTICKET;
    }

    @Override
    public AuthenticationCommand createCommand(Authentication authentication, QueryResponse token)
        throws AuthenticationException {
        final long before = System.currentTimeMillis();

        final String applId = authentication.getApplid();
        final String userId = token.getUserId();
        String passTicket;
        try {
            passTicket = passTicketService.generate(userId, applId);
        } catch (IRRPassTicketGenerationException e) {
            throw new AuthenticationException(
                String.format("Could not generate PassTicket for user ID %s and APPLID %s", userId, applId), e);
        }
        final String encoded = Base64.getEncoder()
            .encodeToString((userId + ":" + passTicket).getBytes(StandardCharsets.UTF_8));
        final String value = "Basic " + encoded;

        final long expiredAt = Math.min(before + authConfigurationProperties.getPassTicket().getTimeout() * 1000,
            token.getExpiration().getTime());

        return new PassTicketCommand(value, expiredAt);
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class PassTicketCommand extends AuthenticationCommand {

        private static final long serialVersionUID = 3941300386857998443L;

        private final String authorizationValue;
        private final long expireAt;

        @Override
        public void apply(InstanceInfo instanceInfo) {
            final RequestContext context = RequestContext.getCurrentContext();
            context.addZuulRequestHeader(HttpHeaders.AUTHORIZATION, authorizationValue);
        }

        @Override
        public boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }

    }
}
