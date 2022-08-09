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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This filter processes certificates on request. It decides, which certificates are considered for client authentication
 */
@RequiredArgsConstructor
@Slf4j
public class CategorizeCertsFilter extends OncePerRequestFilter {

    private static final String ATTRNAME_CLIENT_AUTH_X509_CERTIFICATE = "client.auth.X509Certificate";
    private static final String ATTRNAME_JAVAX_SERVLET_REQUEST_X509_CERTIFICATE = "javax.servlet.request.X509Certificate";
    private static final String LOG_FORMAT_FILTERING_CERTIFICATES = "Filtering certificates: {} -> {}";

    private final Set<String> publicKeyCertificatesBase64;

    public Set<String> getPublicKeyCertificatesBase64() {
        return publicKeyCertificatesBase64;
    }

    /**
     * Get certificates from request (if exists), separate them (to use only APIML certificate to request sign and
     * other for authentication) and store again into request.
     *
     * @param request Request to filter certificates
     */
    private void categorizeCerts(ServletRequest request) {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute(ATTRNAME_JAVAX_SERVLET_REQUEST_X509_CERTIFICATE);
        if (certs != null) {
            request.setAttribute(ATTRNAME_CLIENT_AUTH_X509_CERTIFICATE, selectCerts(certs, certificateForClientAuth));
            request.setAttribute(ATTRNAME_JAVAX_SERVLET_REQUEST_X509_CERTIFICATE, selectCerts(certs, notCertificateForClientAuth));
            log.debug(LOG_FORMAT_FILTERING_CERTIFICATES, ATTRNAME_CLIENT_AUTH_X509_CERTIFICATE, request.getAttribute(ATTRNAME_CLIENT_AUTH_X509_CERTIFICATE));
            log.debug(LOG_FORMAT_FILTERING_CERTIFICATES, ATTRNAME_JAVAX_SERVLET_REQUEST_X509_CERTIFICATE, request.getAttribute(ATTRNAME_JAVAX_SERVLET_REQUEST_X509_CERTIFICATE));
        }
    }

    /**
     * This filter removes all certificates in attribute "javax.servlet.request.X509Certificate" which has no relations
     * with private certificate of apiml and then call original implementation (without "foreign" certificates)
     *
     * @param request  request to process
     * @param response response of call
     * @param filterChain    chain of filters to evaluate
     **/
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        categorizeCerts(request);
        filterChain.doFilter(request, response);
    }

    private X509Certificate[] selectCerts(X509Certificate[] certs, Predicate<X509Certificate> test) {
        return Arrays.stream(certs)
            .filter(test)
            .collect(Collectors.toList()).toArray(new X509Certificate[0]);
    }

    public String base64EncodePublicKey(X509Certificate cert) {
        return Base64.getEncoder().encodeToString(cert.getPublicKey().getEncoded());
    }

    public void setCertificateForClientAuth(Predicate<X509Certificate> certificateForClientAuth) {
        this.certificateForClientAuth = certificateForClientAuth;
    }

    public void setNotCertificateForClientAuth(Predicate<X509Certificate> notCertificateForClientAuth) {
        this.notCertificateForClientAuth = notCertificateForClientAuth;
    }

    Predicate<X509Certificate> certificateForClientAuth = crt -> !getPublicKeyCertificatesBase64().contains(base64EncodePublicKey(crt));
    Predicate<X509Certificate> notCertificateForClientAuth = crt -> getPublicKeyCertificatesBase64().contains(base64EncodePublicKey(crt));


}
