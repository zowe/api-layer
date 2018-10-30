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

import com.ca.mfaas.gateway.security.token.TokenAuthentication;
import com.ca.mfaas.product.config.MFaaSConfigPropertiesContainer;
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
    private final ObjectMapper mapper;
    private final MFaaSConfigPropertiesContainer propertiesContainer;
    private static final String JSON = "JSON";
    private static final String COOKIE = "COOKIE";
    private static final String COOKIE_RESPONSE = "";

    public SuccessfulLoginHandler(ObjectMapper securityObjectMapper,
                                  MFaaSConfigPropertiesContainer propertiesContainer) {
        this.mapper = securityObjectMapper;
        this.propertiesContainer = propertiesContainer;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
        throws IOException {
        TokenAuthentication tokenAuthentication = (TokenAuthentication) authentication;
        String token = tokenAuthentication.getCredentials();

        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        response.setStatus(HttpStatus.OK.value());

        final String responseType = request.getHeader(propertiesContainer.getSecurity().getAuthenticationResponseTypeHeaderName());
        switch (responseType == null ? JSON : responseType.toUpperCase()) {
            case JSON:
                setJson(token, response);
                break;
            case COOKIE:
                setCookie(token, response);
                break;
            default:
                setJson(token, response);
        }
    }

    /**
     * Add the token to the response
     *
     * @param token    the authentication token
     * @param response send back this response
     */
    private void setJson(String token, HttpServletResponse response) throws IOException {
        mapper.writeValue(response.getWriter(), new LoginResponse(token));
        response.getWriter().flush();
        if (!response.isCommitted()) {
            throw new IOException("Setting Authentication response is not commited.");
        }
    }

    /**
     * Add the cookie to the response
     *
     * @param token    the authentication token
     * @param response send back this response
     */
    private void setCookie(String token, HttpServletResponse response) throws IOException {
        Cookie tokenCookie = new Cookie(propertiesContainer.getSecurity().getCookieProperties().getCookieName(), token);
        tokenCookie.setComment(propertiesContainer.getSecurity().getCookieProperties().getCookieComment());
        tokenCookie.setPath(propertiesContainer.getSecurity().getCookieProperties().getCookiePath());
        tokenCookie.setHttpOnly(true);
        tokenCookie.setMaxAge(propertiesContainer.getSecurity().getCookieProperties().getCookieMaxAge());
        tokenCookie.setSecure(propertiesContainer.getSecurity().getCookieProperties().isCookieSecure());
        response.addCookie(tokenCookie);
        mapper.writeValue(response.getWriter(), COOKIE_RESPONSE);
    }
}
