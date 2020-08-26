/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.config;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.zowe.apiml.security.common.token.X509AuthenticationToken;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.cert.X509Certificate;

public class X509Filter extends AbstractAuthenticationProcessingFilter {

    private final AuthenticationProvider authenticationProvider;
    private final AuthenticationSuccessHandler successHandler;


    public X509Filter(String endpoint,
                      AuthenticationSuccessHandler successHandler,
                      AuthenticationProvider authenticationProvider) {
        super(endpoint);
        this.authenticationProvider = authenticationProvider;
        this.successHandler = successHandler;

    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        if (!requiresAuthentication(request, response)) {
            chain.doFilter(request, response);

            return;
        }
            Authentication authResult;
            try {
                authResult = attemptAuthentication(request, response);
                if (authResult == null) {
                    chain.doFilter(request, response);
                    return;
                }
            } catch (AuthenticationException failed) {
                chain.doFilter(request, response);
                return;
            }
            successfulAuthentication(request, response, chain, authResult);

    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute("apiml.X509Certificate");
        if (certs != null && certs.length > 0) {
            return this.authenticationProvider.authenticate(new X509AuthenticationToken(certs));
        }
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
        successHandler.onAuthenticationSuccess(request, response, authResult);
    }
}
