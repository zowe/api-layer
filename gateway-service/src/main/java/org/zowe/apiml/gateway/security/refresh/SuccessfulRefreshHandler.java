/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.refresh;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.TokenCreationService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.util.CookieUtil;

import java.io.IOException;


/**
 * Handler for refreshing the issued JWT token
 * The handler gets authenticated TokenAuthentication with previous token in credentials
 * It invalidates the previous token and issues a fresh one
 */
@Component
@RequiredArgsConstructor
public class SuccessfulRefreshHandler implements AuthenticationSuccessHandler {

    private final AuthConfigurationProperties authConfigurationProperties;
    private final AuthenticationService authenticationService;
    private final TokenCreationService tokenCreationService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (authentication instanceof TokenAuthentication) {
            TokenAuthentication tokenAuth = (TokenAuthentication) authentication;

            authenticationService.invalidateJwtToken(tokenAuth.getCredentials(), true);
            String jwtToken = tokenCreationService.createJwtTokenWithoutCredentials(tokenAuth.getPrincipal());
            setCookie(jwtToken, response);
        }
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    private void setCookie(String token, HttpServletResponse response) {
        AuthConfigurationProperties.CookieProperties cp = authConfigurationProperties.getCookieProperties();
        String cookieHeader = new CookieUtil.CookieHeaderBuilder(cp.getCookieName(), token)
            .comment(cp.getCookieComment())
            .path(cp.getCookiePath())
            .sameSite(cp.getCookieSameSite().getValue())
            .maxAge(cp.getCookieMaxAge())
            .httpOnly(true)
            .secure(cp.isCookieSecure())
            .build();

        response.addHeader("Set-Cookie", cookieHeader);
    }
}
