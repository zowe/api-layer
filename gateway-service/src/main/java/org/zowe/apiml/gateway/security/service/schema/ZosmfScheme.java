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
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.security.login.LoginProvider;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.gateway.security.service.schema.source.JwtAuthSource;
import org.zowe.apiml.gateway.security.service.zosmf.ZosmfService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.util.CookieUtil;

import java.util.Date;
import java.util.Optional;

/**
 * This bean provide LTPA token into request. It get LTPA from authentication source (JWT token value is set on logon)
 * and distribute it as cookie.
 */
@Component
@RequiredArgsConstructor
public class ZosmfScheme implements IAuthenticationScheme {

    private final AuthSourceService authSourceService;
    private final AuthConfigurationProperties authConfigurationProperties;
    @Value("${apiml.security.auth.provider}")
    private String authProvider;

    @Override
    public AuthenticationScheme getScheme() {
        return AuthenticationScheme.ZOSMF;
    }

    @Override
    public AuthenticationCommand createCommand(Authentication authentication, AuthSource authSource) {
        if (!LoginProvider.ZOSMF.getValue().equals(authProvider)) {
            throw new AuthenticationSchemeNotSupportedException("ZOSMF authentication scheme is not supported for this API ML instance.");
        }
        final AuthSource.Parsed parsedAuthSource = authSourceService.parse(authSource);
        final Date expiration = parsedAuthSource == null ? null : parsedAuthSource.getExpiration();
        final Long expirationTime = expiration == null ? null : expiration.getTime();
        return new ZosmfCommand(expirationTime);
    }

    @Override
    public Optional<AuthSource> getAuthSource() {
        return authSourceService.getAuthSourceFromRequest();
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false)
    public class ZosmfCommand extends AuthenticationCommand {

        private static final long serialVersionUID = 2284037230674275720L;

        public static final String COOKIE_HEADER = "cookie";

        private final Long expireAt;

        private void setCookie(RequestContext context, String name, String value) {
            context.addZuulRequestHeader(COOKIE_HEADER,
                CookieUtil.setCookie(
                    context.getZuulRequestHeaders().get(COOKIE_HEADER),
                    name,
                    value
                )
            );
        }

        private void removeCookie(RequestContext context, String name) {
            context.addZuulRequestHeader(COOKIE_HEADER,
                CookieUtil.removeCookie(
                    context.getZuulRequestHeaders().get(COOKIE_HEADER),
                    name
                )
            );
        }

        @Override
        public void apply(InstanceInfo instanceInfo) {
            final RequestContext context = RequestContext.getCurrentContext();

            Optional<AuthSource> authSourceOptional = authSourceService.getAuthSourceFromRequest();
            authSourceOptional.ifPresent(authSource -> {
                // client cert needs to be translated to JWT in advance, so we can determine what is the source of it
                if (authSource.getType().equals(AuthSource.AuthSourceType.CLIENT_CERT)) {
                    authSource = new JwtAuthSource(authSourceService.getJWT(authSource));
                }
                // parse authentication source to detect the source (z/OSMF / Zowe)
                AuthSource.Parsed parsedAuthSource = authSourceService.parse(authSource);

                if (AuthSource.Origin.ZOSMF.equals(parsedAuthSource.getOrigin())) {
                    // token is generated by z/OSMF, fix set cookies
                    removeCookie(context, authConfigurationProperties.getCookieProperties().getCookieName());
                    setCookie(context, ZosmfService.TokenType.JWT.getCookieName(), authSourceService.getJWT(authSource));
                } else if (AuthSource.Origin.ZOWE.equals(parsedAuthSource.getOrigin())) {
                    // user use Zowe own JWT token, for communication with z/OSMF there should be LTPA token, use it
                    final String ltpaToken = authSourceService.getLtpaToken(authSource);
                    setCookie(context, ZosmfService.TokenType.LTPA.getCookieName(), ltpaToken);
                }

                // remove authentication part
                context.addZuulRequestHeader(HttpHeaders.AUTHORIZATION, null);
            });
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
