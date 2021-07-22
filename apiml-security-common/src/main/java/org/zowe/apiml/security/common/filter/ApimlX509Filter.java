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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter;

import javax.servlet.*;
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
public class ApimlX509Filter extends X509AuthenticationFilter {

    private static final String ATTRNAME_CLIENT_AUTH_X509_CERTIFICATE = "client.auth.X509Certificate";
    private static final String ATTRNAME_JAVAX_SERVLET_REQUEST_X509_CERTIFICATE = "javax.servlet.request.X509Certificate";
    private static final String LOG_FORMAT_FILTERING_CERTIFICATES = "Filtering certificates: {} -> {}";

    private final Set<String> publicKeyCertificatesBase64;

    private Set<String> getPublicKeyCertificatesBase64() {
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
     * ApimlX509AuthenticationFilter override methods {@link X509AuthenticationFilter#doFilter(ServletRequest, ServletResponse, FilterChain)}.
     * This filter removes all certificates in attribute "javax.servlet.request.X509Certificate" which has no relations
     * with private certificate of apiml and then call original implementation (without "foreign" certificates)
     *
     * @param request  request to process
     * @param response response of call
     * @param chain    chain of filters to evaluate
     **/
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
            categorizeCerts(request);
        super.doFilter(request, response, chain);
    }

    private X509Certificate[] selectCerts(X509Certificate[] certs, Predicate<X509Certificate> test) {
        return Arrays.stream(certs)
            .filter(test)
            .collect(Collectors.toList()).toArray(new X509Certificate[0]);
    }

    private String base64EncodePublicKey(X509Certificate cert) {
        return Base64.getEncoder().encodeToString(cert.getPublicKey().getEncoded());
    }

    Predicate<X509Certificate> certificateForClientAuth = crt -> !getPublicKeyCertificatesBase64().contains(base64EncodePublicKey(crt));
    Predicate<X509Certificate> notCertificateForClientAuth = crt -> getPublicKeyCertificatesBase64().contains(base64EncodePublicKey(crt));
}
