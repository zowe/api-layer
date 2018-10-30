/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.security.login;

import com.ca.mfaas.security.token.CookieConfiguration;
import com.ca.mfaas.security.token.TokenAuthentication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private final ObjectMapper mapper;
    private final CookieConfiguration cookieConfiguration;

    public SuccessfulLoginHandler(@Qualifier("securityObjectMapper") ObjectMapper mapper,
                                  CookieConfiguration cookieConfiguration) {
        this.mapper = mapper;
        this.cookieConfiguration = cookieConfiguration;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
        throws IOException {
        TokenAuthentication tokenAuthentication = (TokenAuthentication) authentication;
        String token = tokenAuthentication.getCredentials();

        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        response.setStatus(HttpStatus.OK.value());

        Cookie tokenCookie = new Cookie(cookieConfiguration.getName(), token);
        tokenCookie.setComment(cookieConfiguration.getComment());
        tokenCookie.setPath(cookieConfiguration.getPath());
        tokenCookie.setHttpOnly(true);
        tokenCookie.setMaxAge(cookieConfiguration.getMaxAge());
        tokenCookie.setSecure(cookieConfiguration.isSecure());
        response.addCookie(tokenCookie);

        mapper.writeValue(response.getWriter(), "");
    }
}
