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

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.verify.CertificateValidator;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
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
    private static final String CLIENT_CERT_HEADER = "Client-Cert";

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    @Getter
    private final Set<String> publicKeyCertificatesBase64;

    private final CertificateValidator certificateValidator;

    /**
     * Get certificates from request (if exists), separate them (to use only APIML certificate to request sign and
     * other for authentication) and store again into request.
     * If authentication via certificate in header is enabled, get client certificate from the Client-Cert header
     * only if the request certificates are all trusted and validated by {@link CertificateValidator} service.
     * The client certificate is then stored in a separate custom request attribute. The APIML certificates stays
     * in the default attribute.
     *
     * @param request Request to filter certificates
     */
    private void categorizeCerts(ServletRequest request) {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute(ATTRNAME_JAVAX_SERVLET_REQUEST_X509_CERTIFICATE);
        if (certs != null) {
            if (certificateValidator.isForwardingEnabled() && certificateValidator.isTrusted(certs)) {
                certificateValidator.updateAPIMLPublicKeyCertificates(certs);
                Optional<Certificate> clientCert = getClientCertFromHeader((HttpServletRequest) request);
                if (clientCert.isPresent()) {
                    // add the client certificate to the certs array
                    String subjectDN = ((X509Certificate) clientCert.get()).getSubjectX500Principal().getName();
                    log.debug("Found client certificate in header, adding it to the request. Subject DN: {}", subjectDN);
                    certs = Arrays.copyOf(certs, certs.length + 1);
                    certs[certs.length - 1] = (X509Certificate) clientCert.get();
                }
            }

            request.setAttribute(ATTRNAME_CLIENT_AUTH_X509_CERTIFICATE, selectCerts(certs, certificateForClientAuth));
            request.setAttribute(ATTRNAME_JAVAX_SERVLET_REQUEST_X509_CERTIFICATE, selectCerts(certs, apimlCertificate));
            log.debug(LOG_FORMAT_FILTERING_CERTIFICATES, ATTRNAME_CLIENT_AUTH_X509_CERTIFICATE, request.getAttribute(ATTRNAME_CLIENT_AUTH_X509_CERTIFICATE));
        }
    }

    private Optional<Certificate> getClientCertFromHeader(HttpServletRequest request) {
        String certFromHeader = request.getHeader(CLIENT_CERT_HEADER);

        if (StringUtils.isNotEmpty(certFromHeader)) {
            try {
                Certificate certificate = CertificateFactory
                    .getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(Base64.getDecoder().decode(certFromHeader)));
                return Optional.of(certificate);
            } catch (Exception e) {
                apimlLog.log("org.zowe.apiml.security.common.filter.errorParsingCertificate", e.getMessage(), certFromHeader);
            }
        }
        return Optional.empty();
    }

    /**
     * Wraps the http servlet request into wrapper which makes sure that the Client-Cert header
     * will not be accessible anymore from the request
     *
     * @param originalRequest incoming original http request object
     * @return wrapped http request object with overridden functions
     */
    private HttpServletRequest mutate(HttpServletRequest originalRequest) {
        return new HttpServletRequestWrapper(originalRequest) {

            @Override
            public String getHeader(String name) {
                if (CLIENT_CERT_HEADER.equalsIgnoreCase(name)) {
                    return null;
                }
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if (CLIENT_CERT_HEADER.equalsIgnoreCase(name)) {
                    return Collections.enumeration(new LinkedList<>());
                }
                return super.getHeaders(name);
            }
        };
    }

    /**
     * This filter removes all certificates in attribute "javax.servlet.request.X509Certificate" which has no relations
     * with private certificate of apiml and then call original implementation (without "foreign" certificates)
     *
     * @param request     request to process
     * @param response    response of call
     * @param filterChain chain of filters to evaluate
     **/
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        categorizeCerts(request);
        filterChain.doFilter(mutate(request), response);
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

    public void setApimlCertificate(Predicate<X509Certificate> apimlCertificate) {
        this.apimlCertificate = apimlCertificate;
    }

    Predicate<X509Certificate> certificateForClientAuth = crt -> !getPublicKeyCertificatesBase64().contains(base64EncodePublicKey(crt));
    Predicate<X509Certificate> apimlCertificate = crt -> getPublicKeyCertificatesBase64().contains(base64EncodePublicKey(crt));


}
