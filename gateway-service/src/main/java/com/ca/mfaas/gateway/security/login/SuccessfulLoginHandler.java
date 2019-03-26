/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.login;

import com.ca.mfaas.gateway.security.config.SecurityConfigurationProperties;
import com.ca.apiml.security.token.TokenAuthentication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

@Component
public class SuccessfulLoginHandler implements AuthenticationSuccessHandler {
    private static final String COOKIE_RESPONSE = "";

    private final ObjectMapper mapper;
    private final SecurityConfigurationProperties securityConfigurationProperties;

    public SuccessfulLoginHandler(ObjectMapper securityObjectMapper,
                                  SecurityConfigurationProperties securityConfigurationProperties) {
        this.mapper = securityObjectMapper;
        this.securityConfigurationProperties = securityConfigurationProperties;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
        throws IOException {
        TokenAuthentication tokenAuthentication = (TokenAuthentication) authentication;
        String token = tokenAuthentication.getCredentials();

        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        response.setStatus(HttpStatus.OK.value());

        setCookie(token, response);
    }

    /**
     * Add the cookie to the response
     *
     * @param token    the authentication token
     * @param response send back this response
     */
    private void setCookie(String token, HttpServletResponse response) throws IOException {
        Cookie tokenCookie = new Cookie(securityConfigurationProperties.getCookieProperties().getCookieName(), token);
        tokenCookie.setComment(securityConfigurationProperties.getCookieProperties().getCookieComment());
        tokenCookie.setPath(securityConfigurationProperties.getCookieProperties().getCookiePath());
        tokenCookie.setHttpOnly(true);
        tokenCookie.setMaxAge(securityConfigurationProperties.getCookieProperties().getCookieMaxAge());
        tokenCookie.setSecure(securityConfigurationProperties.getCookieProperties().isCookieSecure());

        response.addCookie(tokenCookie);
        mapper.writeValue(response.getWriter(), COOKIE_RESPONSE);
    }
}
