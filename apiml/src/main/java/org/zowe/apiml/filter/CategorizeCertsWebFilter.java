/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.filter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.security.common.verify.CertificateValidator; // Assuming this dependency is available

import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.function.Predicate;

/**
 * This GlobalFilter processes client certificates present in the TLS handshake or in a specific header.
 * It categorizes these certificates for client authentication and for identifying APIML (gateway) internal certificates.
 * It also removes the client certificate header before forwarding the request downstream.
 */
@RequiredArgsConstructor
@Slf4j
public class CategorizeCertsWebFilter implements WebFilter, Ordered {

    // Attribute name for client certificates intended for authentication.
    public static final String ATTR_NAME_CLIENT_AUTH_X509_CERTIFICATE = "client.auth.X509Certificate";
    public static final String ATTR_NAME_GATEWAY_REQUEST_X509_CERTIFICATE = "gateway.request.X509Certificate"; // Renamed from jakarta.servlet...
    private static final String LOG_FORMAT_FILTERING_CERTIFICATES = "Filtering certificates for attribute {}: {}";
    private static final String CLIENT_CERT_HEADER = "Client-Cert"; // Header to check for client certificate

    // Injected via constructor by Lombok's @RequiredArgsConstructor
    @Getter
    private final Set<String> publicKeyCertificatesBase64; // Base64 encoded public keys of APIML certificates
    private final CertificateValidator certificateValidator; // Service for validating certificates

    // Predicate to identify certificates used for client authentication (i.e., not APIML certificates)
    @Setter
    private Predicate<X509Certificate> certificateForClientAuth = cert ->
        !getPublicKeyCertificatesBase64().contains(base64EncodePublicKey(cert));

    // Predicate to identify APIML (gateway) certificates
    @Setter
    private Predicate<X509Certificate> apimlCertificate = cert ->
        getPublicKeyCertificatesBase64().contains(base64EncodePublicKey(cert));

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        categorizeCerts(exchange);
        ServerHttpRequest mutatedRequest = mutateRequestToRemoveHeader(exchange.getRequest());
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * Categorizes certificates from the TLS handshake and/or a specific header.
     * Stores categorized certificates in the exchange attributes.
     *
     * @param exchange The current server web exchange.
     */
    private void categorizeCerts(ServerWebExchange exchange) {
        X509Certificate[] certsFromTls = null;
        if (exchange.getRequest().getSslInfo() != null &&
            exchange.getRequest().getSslInfo().getPeerCertificates() != null) {
            certsFromTls = exchange.getRequest().getSslInfo().getPeerCertificates();
        }

        if (certsFromTls != null && certsFromTls.length > 0 && certsFromTls[0] != null) {
            Optional<X509Certificate> clientCertFromHeader = getClientCertFromHeader(exchange.getRequest());

            if (certificateValidator.isForwardingEnabled() &&
                certificateValidator.hasGatewayChain(certsFromTls) &&
                clientCertFromHeader.isPresent()) {

                // This call might update a shared state within CertificateValidator or rely on it.
                certificateValidator.updateAPIMLPublicKeyCertificates(certsFromTls);

                X509Certificate[] clientAuthCerts = selectCerts(
                    new X509Certificate[]{clientCertFromHeader.get()},
                    certificateForClientAuth
                );
                exchange.getAttributes().put(ATTR_NAME_CLIENT_AUTH_X509_CERTIFICATE, clientAuthCerts);
                log.debug(LOG_FORMAT_FILTERING_CERTIFICATES, ATTR_NAME_CLIENT_AUTH_X509_CERTIFICATE, Arrays.toString(clientAuthCerts));

                // In this branch, the original filter did not modify the "jakarta.servlet.request.X509Certificate" attribute.
                // So, if any downstream filter expects original TLS certs under ATTR_NAME_GATEWAY_REQUEST_X509_CERTIFICATE,
                // they would find the full chain here. Or, we can explicitly set it.
                // For clarity and consistency with the 'else' branch, we can set it to the original TLS certs.
                exchange.getAttributes().put(ATTR_NAME_GATEWAY_REQUEST_X509_CERTIFICATE, certsFromTls);
                log.debug("Retaining full TLS certificate chain in attribute {}: {}", ATTR_NAME_GATEWAY_REQUEST_X509_CERTIFICATE, Arrays.toString(certsFromTls));


            } else {
                X509Certificate[] clientAuthCerts = selectCerts(certsFromTls, certificateForClientAuth);
                exchange.getAttributes().put(ATTR_NAME_CLIENT_AUTH_X509_CERTIFICATE, clientAuthCerts);
                log.debug(LOG_FORMAT_FILTERING_CERTIFICATES, ATTR_NAME_CLIENT_AUTH_X509_CERTIFICATE, Arrays.toString(clientAuthCerts));

                X509Certificate[] apimlFilteredCerts = selectCerts(certsFromTls, apimlCertificate);
                exchange.getAttributes().put(ATTR_NAME_GATEWAY_REQUEST_X509_CERTIFICATE, apimlFilteredCerts);
                log.debug(LOG_FORMAT_FILTERING_CERTIFICATES, ATTR_NAME_GATEWAY_REQUEST_X509_CERTIFICATE, Arrays.toString(apimlFilteredCerts));
            }
        } else {
            log.debug("No TLS peer certificates found in the request.");
        }
    }

    /**
     * Extracts and decodes an X.509 certificate from the CLIENT_CERT_HEADER.
     *
     * @param request The current server HTTP request.
     * @return An Optional containing the X509Certificate if found and valid, otherwise empty.
     */
    private Optional<X509Certificate> getClientCertFromHeader(ServerHttpRequest request) {
        String certFromHeader = request.getHeaders().getFirst(CLIENT_CERT_HEADER);

        if (StringUtils.isNotEmpty(certFromHeader)) {
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                Certificate certificate = cf.generateCertificate(
                    new ByteArrayInputStream(Base64.getDecoder().decode(certFromHeader))
                );
                if (certificate instanceof X509Certificate x509certificate) {
                    log.debug("Successfully parsed X.509 certificate from header {}.", CLIENT_CERT_HEADER);
                    return Optional.of(x509certificate);
                } else {
                    log.warn("Certificate parsed from header {} is not an X.509 certificate.", CLIENT_CERT_HEADER);
                }
            } catch (CertificateException | IllegalArgumentException e) {
                // Logged as error to match original ApimlLogger behavior for "errorParsingCertificate"
                log.error("Error parsing X.509 certificate from header '{}'. Remote host: {}. Message: {}. Certificate data: '{}'",
                    CLIENT_CERT_HEADER,
                    request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress() : "N/A",
                    e.getMessage(),
                    certFromHeader.substring(0, Math.min(certFromHeader.length(), 100)) + "...", // Log a snippet
                    e);
            }
        }
        return Optional.empty();
    }

    /**
     * Creates a new ServerHttpRequest with the CLIENT_CERT_HEADER removed.
     *
     * @param originalRequest The original server HTTP request.
     * @return A new ServerHttpRequest instance without the CLIENT_CERT_HEADER.
     */
    private ServerHttpRequest mutateRequestToRemoveHeader(ServerHttpRequest originalRequest) {
        return originalRequest.mutate()
            .headers(httpHeaders -> httpHeaders.remove(CLIENT_CERT_HEADER))
            .build();
    }

    /**
     * Selects certificates from an array based on a predicate.
     * IMPORTANT: This method replicates the original filter's logic. If the first certificate
     * in the array matches the predicate, the entire original array is returned.
     * Otherwise, an empty array is returned. This does not filter individual certificates
     * from the array beyond the first one if the input array contains multiple certificates.
     *
     * @param certs The array of X.509 certificates.
     * @param test  The predicate to test the certificates against.
     * @return An array of X.509 certificates.
     */
    private X509Certificate[] selectCerts(X509Certificate[] certs, Predicate<X509Certificate> test) {
        if (certs != null && certs.length > 0 && certs[0] != null && test.test(certs[0])) {
            return certs; // Returns the whole array if the first cert matches
        }
        return new X509Certificate[0]; // Returns an empty array otherwise
    }

    /*
     * Alternative selectCerts implementation that filters all certificates in the array.
     * Use this if the intention is to get all certificates matching the predicate, not just
     * basing the decision on the first certificate.
     *
    private X509Certificate[] selectCertsAlternatively(X509Certificate[] certs, Predicate<X509Certificate> test) {
        if (certs == null || certs.length == 0) {
            return new X509Certificate[0];
        }
        return Arrays.stream(certs)
                     .filter(Objects::nonNull)
                     .filter(test)
                     .toArray(X509Certificate[]::new);
    }
    */


    /**
     * Encodes the public key of an X.509 certificate to a Base64 string.
     *
     * @param cert The X.509 certificate.
     * @return The Base64 encoded string of the public key.
     */
    public static String base64EncodePublicKey(X509Certificate cert) {
        return Base64.getEncoder().encodeToString(cert.getPublicKey().getEncoded());
    }

    /**
     * Defines the order of this filter. It should run relatively early
     * if it prepares data for subsequent authentication filters.
     *
     * @return The filter order.
     */
    @Override
    public int getOrder() {
        // Example: Run before Spring Security's default authentication filters.
        // Adjust as needed based on your filter chain.
        // A lower number means higher precedence.
        return Ordered.HIGHEST_PRECEDENCE + 100; // Example order
    }
}

