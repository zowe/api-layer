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
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.springframework.stereotype.Component;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.gateway.security.service.schema.source.JwtAuthSource;
import org.zowe.apiml.gateway.security.service.zosmf.ZosmfService;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.util.CookieUtil;
import org.zowe.apiml.util.Cookies;

import java.net.HttpCookie;
import java.util.Optional;

/**
 * This bean provide LTPA token into request. It get LTPA from JWT token (value is set on logon) and distribute it as
 * cookie.
 */
@Component
@AllArgsConstructor
public class ZosmfScheme implements AbstractAuthenticationScheme {

    private final AuthSourceService authSourceService;
    private final AuthConfigurationProperties authConfigurationProperties;

    @Override
    public AuthenticationScheme getScheme() {
        return AuthenticationScheme.ZOSMF;
    }

    @Override
    public AuthenticationCommand createCommand(Authentication authentication, JwtAuthSource authSource) {
        final QueryResponse queryResponse = authSourceService.parse(authSource);
        final Date expiration = queryResponse == null ? null : queryResponse.getExpiration();
        final Long expirationTime = expiration == null ? null : expiration.getTime();
        return new ZosmfCommand(expirationTime);
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false)
    public class ZosmfCommand extends AuthenticationCommand {

        private static final long serialVersionUID = 2284037230674275720L;

        public static final String COOKIE_HEADER = "cookie";

        private final Long expireAt;

        private void createCookie(Cookies cookies, String name, String token) {
            HttpCookie jwtCookie = new HttpCookie(name, token);
            jwtCookie.setSecure(true);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setVersion(0);
            cookies.set(jwtCookie);
        }

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

            Optional<JwtAuthSource> jwtToken = authSourceService.getAuthSource();
            jwtToken.ifPresent(token -> {
                // parse JWT token to detect the source (z/OSMF / Zowe)
                QueryResponse queryResponse = authSourceService.parse(token);
                switch (queryResponse.getSource()) {
                    case ZOSMF:
                        // token is generated by z/OSMF, fix set cookies
                        removeCookie(context, authConfigurationProperties.getCookieProperties().getCookieName());
                        setCookie(context, ZosmfService.TokenType.JWT.getCookieName(), token.getSource());
                        break;
                    case ZOWE:
                        // user use Zowe own JWT token, for communication with z/OSMF there should be LTPA token, use it
                        final String ltpaToken = authSourceService.getLtpaTokenWithValidation(token.getSource());
                        setCookie(context, ZosmfService.TokenType.LTPA.getCookieName(), ltpaToken);
                        break;
                    default:
                        return;
                }

                // remove authentication part
                context.addZuulRequestHeader(HttpHeaders.AUTHORIZATION, null);
            });
        }

        @Override
        public void applyToRequest(HttpRequest request) {
            Cookies cookies = Cookies.of(request);

            Optional<JwtAuthSource> jwtToken = authSourceService.getAuthSource();
            jwtToken.ifPresent(token -> {
                // parse JWT token to detect the source (z/OSMF / Zowe)
                QueryResponse queryResponse = authSourceService.parse(token);
                switch (queryResponse.getSource()) {
                    case ZOSMF:
                        cookies.remove(authConfigurationProperties.getCookieProperties().getCookieName());
                        createCookie(cookies, ZosmfService.TokenType.JWT.getCookieName(), token.getSource());
                        break;
                    case ZOWE:
                        final String ltpaToken = authSourceService.getLtpaTokenWithValidation(token.getSource());
                        createCookie(cookies, ZosmfService.TokenType.LTPA.getCookieName(), ltpaToken);
                        break;
                    default:
                        return;
                }
                // remove authentication part
                request.removeHeaders(HttpHeaders.AUTHORIZATION);
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
