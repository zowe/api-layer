/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.login;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.zowe.apiml.security.common.token.X509AuthenticationToken;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.cert.X509Certificate;

@Slf4j
public class X509AuthenticationFilter extends NonCompulsoryAuthenticationProcessingFilter {

    private final AuthenticationProvider authenticationProvider;
    private final AuthenticationSuccessHandler successHandler;

    public X509AuthenticationFilter(String endpoint,
                                    AuthenticationSuccessHandler successHandler,
                                    AuthenticationProvider authenticationProvider) {
        super(endpoint);
        this.authenticationProvider = authenticationProvider;
        this.successHandler = successHandler;


    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute("client.auth.X509Certificate");
        if (certs != null && certs.length > 0) {
            log.debug("One or more X509 certificate found in request.");
            return this.authenticationProvider.authenticate(new X509AuthenticationToken(certs));
        }
        log.debug("No X509 certificate found in request.");
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
