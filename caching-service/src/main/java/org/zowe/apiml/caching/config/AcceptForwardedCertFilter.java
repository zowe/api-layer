/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.config;


import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.verify.CertificateValidator;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.Set;

import static org.zowe.apiml.security.common.filter.CategorizeCertsFilter.getClientCertFromHeader;
import static org.zowe.apiml.security.common.filter.CategorizeCertsFilter.mutate;

/**
 * This filter processes certificates on request. It decides, which certificates are considered for client authentication
 */
@RequiredArgsConstructor
@Slf4j
public class AcceptForwardedCertFilter extends OncePerRequestFilter {

    private static final String ATTRNAME_CLIENT_AUTH_X509_CERTIFICATE = "client.auth.X509Certificate";
    private static final String ATTRNAME_JAVAX_SERVLET_REQUEST_X509_CERTIFICATE = "javax.servlet.request.X509Certificate";
    private static final String LOG_FORMAT_FILTERING_CERTIFICATES = "Filtering certificates: {} -> {}";

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    @Getter
    private final Set<String> publicKeyCertificatesBase64;

    private final CertificateValidator certificateValidator;

    /**
     * Resolves which certificate should be used for client authentication and stores it in the
     * {@code client.auth.X509Certificate} request attribute.
     *
     * <p>Two paths are possible:
     * <ul>
     *   <li><b>Forwarding enabled and TLS certs trusted</b> — the request arrived from a trusted upstream
     *       gateway. The actual end-user certificate is read from the {@code Client-Cert} header (placed
     *       there by the upstream gateway) and stored as the client auth certificate.</li>
     *   <li><b>Otherwise</b> — the TLS handshake certificates from
     *       {@code javax.servlet.request.X509Certificate} are used directly as the client auth
     *       certificate, meaning the caller authenticated via mutual TLS.</li>
     * </ul>
     *
     * <p>If no TLS certificates are present on the request at all, the method is a no-op.
     *
     * @param request the incoming servlet request
     */
    private void categorizeCerts(ServletRequest request) {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute(ATTRNAME_JAVAX_SERVLET_REQUEST_X509_CERTIFICATE);
        if (certs != null) {
            if (certificateValidator.isForwardingEnabled() && certificateValidator.isTrusted(certs)) {

                Optional<Certificate> clientCert = getClientCertFromHeader((HttpServletRequest) request, apimlLog);
                if (clientCert.isPresent()) {
                    // add the client certificate to the certs array
                    String subjectDN = ((X509Certificate) clientCert.get()).getSubjectX500Principal().getName();
                    log.debug("Found client certificate in header, adding it to the request. Subject DN: {}", subjectDN);
                    X509Certificate[] cc = new X509Certificate[1];
                    cc[0] = (X509Certificate) clientCert.get();
                    request.setAttribute(ATTRNAME_CLIENT_AUTH_X509_CERTIFICATE, cc);
                }
            } else {
                request.setAttribute(ATTRNAME_CLIENT_AUTH_X509_CERTIFICATE, request.getAttribute(ATTRNAME_JAVAX_SERVLET_REQUEST_X509_CERTIFICATE));
            }
            log.debug(LOG_FORMAT_FILTERING_CERTIFICATES, ATTRNAME_CLIENT_AUTH_X509_CERTIFICATE, request.getAttribute(ATTRNAME_CLIENT_AUTH_X509_CERTIFICATE));
        }
    }

    /**
     * Categorizes the client certificate for the request and then continues the filter chain.
     *
     * <p>The request is wrapped via {@code mutate} before being passed downstream so that the
     * raw {@code Client-Cert} header is no longer accessible to subsequent filters or the
     * application — preventing the forwarded certificate value from leaking into business logic
     * that should only see the resolved {@code client.auth.X509Certificate} attribute.
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response
     * @param filterChain the remaining filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        categorizeCerts(request);
        filterChain.doFilter(mutate(request), response);
    }

}

