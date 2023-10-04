/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters.pre;

import com.netflix.zuul.context.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.JwtAuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.JwtAuthSourceService;
import org.zowe.apiml.security.common.login.NonCompulsoryAuthenticationProcessingFilter;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public class JWTAuthenticationFilter extends NonCompulsoryAuthenticationProcessingFilter {
    private final AuthenticationFailureHandler failureHandler;

    private final JwtAuthSourceService jwtAuthSourceService;

       public JWTAuthenticationFilter(String endpoint,
                                    AuthenticationFailureHandler failureHandler,
                                      JwtAuthSourceService jwtAuthSourceService) {
        super(endpoint);
        this.failureHandler = failureHandler;
        this.jwtAuthSourceService = jwtAuthSourceService;
    }
        @Override
        public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {

           RequestContext context =  (RequestContext)request;

            String jwtToken = String.valueOf(jwtAuthSourceService.getToken(context));


            if (jwtToken != null && !jwtToken.isEmpty()) {
                log.debug("jwt access token found in the request");
                AuthSource authSource = new JwtAuthSource(jwtToken);

                AuthSource.Parsed parsedToken = jwtAuthSourceService.parse(authSource);
                if (parsedToken != null && StringUtils.isNotEmpty(parsedToken.getUserId())) {
                    return TokenAuthentication.createAuthenticated(parsedToken.getUserId(), jwtToken);
                }
            }

            log.debug("No valid jwt access token found in the request.");
            return null;
        }

        /**
         * Calls successful login handler
         */
        @Override
        protected void successfulAuthentication(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain,
            Authentication authResult) throws IOException, ServletException {
            if (SecurityContextHolder.getContext().getAuthentication() == null || !SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authResult);
                SecurityContextHolder.setContext(context);
            }
            chain.doFilter(request, response);
        }

        @Override
        protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException
        failed) throws IOException, ServletException {
            failureHandler.onAuthenticationFailure(request, response, failed);
        }
}
