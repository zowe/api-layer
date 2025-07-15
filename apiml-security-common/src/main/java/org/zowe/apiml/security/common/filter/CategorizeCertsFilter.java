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
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.verify.CertificateValidator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.function.Predicate;

/**
 * This filter processes certificates on request. It decides, which certificates are considered for client authentication
 */
@RequiredArgsConstructor
@Slf4j
public class CategorizeCertsFilter extends OncePerRequestFilter {

    public static final String ATTR_NAME_CLIENT_AUTH_X509_CERTIFICATE = "client.auth.X509Certificate";
    public static final String ATTR_NAME_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE = "jakarta.servlet.request.X509Certificate";
    public static final String LOG_FORMAT_FILTERING_CERTIFICATES = "Filtering certificates: {} -> {}";
    public static final String CLIENT_CERT_HEADER = "Client-Cert";

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
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute(ATTR_NAME_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE);
        if (certs != null && certs.length > 0) {
            Optional<Certificate> clientCert = getClientCertFromHeader((HttpServletRequest) request);
            if (certificateValidator.isForwardingEnabled() && certificateValidator.hasGatewayChain(certs) && clientCert.isPresent()) {
                certificateValidator.updateAPIMLPublicKeyCertificates(certs);
                // add the client certificate to the certs array
                String subjectDN = ((X509Certificate) clientCert.get()).getSubjectX500Principal().getName();
                log.debug("Found client certificate in header, adding it to the request. Subject DN: {}", subjectDN);
                request.setAttribute(ATTR_NAME_CLIENT_AUTH_X509_CERTIFICATE, selectCerts(new X509Certificate[]{(X509Certificate) clientCert.get()}, certificateForClientAuth));
            } else {
                request.setAttribute(ATTR_NAME_CLIENT_AUTH_X509_CERTIFICATE, selectCerts(certs, certificateForClientAuth));
                request.setAttribute(ATTR_NAME_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE, selectCerts(certs, apimlCertificate));
            }

            log.debug(LOG_FORMAT_FILTERING_CERTIFICATES, ATTR_NAME_CLIENT_AUTH_X509_CERTIFICATE, request.getAttribute(ATTR_NAME_CLIENT_AUTH_X509_CERTIFICATE));
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
                apimlLog.log("org.zowe.apiml.security.common.filter.errorParsingCertificate", request.getRemoteHost(), e.getMessage(), certFromHeader);
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
     * This filter removes all certificates in attribute "jakarta.servlet.request.X509Certificate" which has no relations
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

    /**
     * Selects certificates from an array based on a predicate.
     * Only certificates that match the predicate will be included in the result array.
     * IMPORTANT: This method replicates the original filter's logic.
     * <p>
     * Null certificates are automatically excluded.
     *
     * @param certs The array of X.509 certificates to be filtered.
     * @param test  The predicate to test each certificate against.
     * @return A new array containing only the certificates that match the predicate.
     */
    public static X509Certificate[] selectCerts(X509Certificate[] certs, Predicate<X509Certificate> test) {
        return Optional.ofNullable(certs)
            .stream()
            .flatMap(Arrays::stream)
            .filter(Objects::nonNull)
            .filter(test)
            .toArray(X509Certificate[]::new);
    }

    public static String base64EncodePublicKey(X509Certificate cert) {
        return Base64.getEncoder().encodeToString(cert.getPublicKey().getEncoded());
    }

    @Setter
    Predicate<X509Certificate> certificateForClientAuth = crt -> !getPublicKeyCertificatesBase64().contains(base64EncodePublicKey(crt));
    @Setter
    Predicate<X509Certificate> apimlCertificate = crt -> getPublicKeyCertificatesBase64().contains(base64EncodePublicKey(crt));

}
