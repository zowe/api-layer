/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;
import org.zowe.apiml.security.common.error.AuthExceptionHandler;
import org.zowe.apiml.security.common.error.InvalidCertificateException;

import java.io.IOException;
import java.security.cert.X509Certificate;

@Slf4j
@RequiredArgsConstructor
public class X509ClientRejectIfMissingFilter extends OncePerRequestFilter {

    static final String ATTRNAME_CLIENT_AUTH_X509_CERTIFICATE = "client.auth.X509Certificate";

    private final AuthExceptionHandler authExceptionHandler;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute(ATTRNAME_CLIENT_AUTH_X509_CERTIFICATE);
        if (certs == null || certs.length == 0) {
            log.debug("No X509 client certificate found in request.");
            authExceptionHandler.handleException(request, response, new InvalidCertificateException("X509 client certificate required but missing."));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
