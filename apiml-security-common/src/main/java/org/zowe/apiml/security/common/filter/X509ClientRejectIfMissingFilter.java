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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.zowe.apiml.security.common.error.AuthExceptionHandler;
import org.zowe.apiml.security.common.error.InvalidCertificateException;
import org.zowe.apiml.security.common.verify.CertificateValidator;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.HashSet;

/**
 * Checks the client certificate is present in the request. No further validation is done, the client certificate
 * was already validated by Gateway. Servers as a counterpart to {@link CategorizeCertsFilter} for simple scenarios
 * when client certificate is required to be present but is not used for authorization.
 */

@Slf4j
public class X509ClientRejectIfMissingFilter extends CategorizeCertsFilter {

    public X509ClientRejectIfMissingFilter(CertificateValidator certificateValidator, AuthExceptionHandler authExceptionHandler) {
        super(new HashSet<>(), certificateValidator, authExceptionHandler, true);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var certOpt = getClientCertFromHeader(request);
        if (certOpt.isEmpty()) {
            log.debug("No X509 client certificate found in request.");
            authExceptionHandler.handleException(request, response, new InvalidCertificateException("X509 client certificate required but missing."));
            return;
        }

        var cert = (X509Certificate) certOpt.get();
        var auth = new PreAuthenticatedAuthenticationToken(cert.getSubjectX500Principal(), cert);
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(mutate(request), response);
    }
}
