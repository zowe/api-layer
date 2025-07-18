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

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.verify.CertificateValidator;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.function.Predicate;

import static org.zowe.apiml.security.common.filter.CategorizeCertsFilter.*;


/**
 * This GlobalFilter processes client certificates present in the TLS handshake or in a specific header.
 * It categorizes these certificates for client authentication and for identifying APIML (gateway) internal certificates.
 * It also removes the client certificate header before forwarding the request downstream.
 */
@RequiredArgsConstructor
@Slf4j
public class CategorizeCertsWebFilter implements WebFilter, Ordered {

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    @Getter
    private final Set<String> publicKeyCertificatesBase64;
    private final CertificateValidator certificateValidator;

    @Setter
    private Predicate<X509Certificate> certificateForClientAuth = cert ->
        !getPublicKeyCertificatesBase64().contains(base64EncodePublicKey(cert));

    @Setter
    private Predicate<X509Certificate> apimlCertificate = cert ->
        getPublicKeyCertificatesBase64().contains(base64EncodePublicKey(cert));

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerWebExchange updatedExchange = categorizeCerts(exchange);
        ServerHttpRequest mutatedRequest = mutateRequestToRemoveHeader(updatedExchange.getRequest());
        return chain.filter(updatedExchange.mutate().request(mutatedRequest).build());
    }

    /**
     * Categorizes certificates from the TLS handshake and/or a specific header.
     * Stores categorized certificates in the exchange attributes.
     *
     * @param exchange The current server web exchange.
     */
    private ServerWebExchange categorizeCerts(ServerWebExchange exchange) {
        Optional<X509Certificate[]> certsFromTlsOpt = Optional.of(exchange)
            .map(ServerWebExchange::getRequest)
            .map(ServerHttpRequest::getSslInfo)
            .map(SslInfo::getPeerCertificates)
            .filter(Objects::nonNull)
            .filter(ssl -> ssl.length > 0);

        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();

        certsFromTlsOpt.ifPresent(certsFromTls -> {
            Optional<X509Certificate> clientCertFromHeader = getClientCertFromHeader(exchange.getRequest());

            if (certificateValidator.isForwardingEnabled() &&
                certificateValidator.hasGatewayChain(certsFromTls) &&
                clientCertFromHeader.isPresent()) {

                certificateValidator.updateAPIMLPublicKeyCertificates(certsFromTls);

                X509Certificate[] clientAuthCerts = selectCerts(
                    new X509Certificate[]{clientCertFromHeader.get()},
                    certificateForClientAuth
                );
                exchange.getAttributes().put(ATTR_NAME_CLIENT_AUTH_X509_CERTIFICATE, clientAuthCerts);
                log.debug(LOG_FORMAT_FILTERING_CERTIFICATES, ATTR_NAME_CLIENT_AUTH_X509_CERTIFICATE, Arrays.toString(clientAuthCerts));

                exchange.getAttributes().put(ATTR_NAME_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE, certsFromTls);
                log.debug("Retaining full TLS certificate chain in attribute {}: {}", ATTR_NAME_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE, Arrays.toString(certsFromTls));

                var sslInfo = SimpleSslInfo.builder().peerCertificates(clientAuthCerts).build();
                requestBuilder.sslInfo(sslInfo);

            } else {
                X509Certificate[] clientAuthCerts = selectCerts(certsFromTls, certificateForClientAuth);
                exchange.getAttributes().put(ATTR_NAME_CLIENT_AUTH_X509_CERTIFICATE, clientAuthCerts);
                log.debug(LOG_FORMAT_FILTERING_CERTIFICATES, ATTR_NAME_CLIENT_AUTH_X509_CERTIFICATE, Arrays.toString(clientAuthCerts));

                X509Certificate[] apimlFilteredCerts = selectCerts(certsFromTls, apimlCertificate);
                exchange.getAttributes().put(ATTR_NAME_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE, apimlFilteredCerts);
                log.debug(LOG_FORMAT_FILTERING_CERTIFICATES, ATTR_NAME_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE, Arrays.toString(apimlFilteredCerts));
                var sslInfo = SimpleSslInfo.builder().peerCertificates(clientAuthCerts).build();
                requestBuilder.sslInfo(sslInfo);

            }

        });

        if (certsFromTlsOpt.isEmpty()) {
            log.debug("No TLS peer certificates found in the request.");
        }
        return exchange.mutate().request(requestBuilder.build()).build();
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
                    log.debug("Successfully parsed X.509 certificate from header {}.", certFromHeader);
                    return Optional.of(x509certificate);
                } else {
                    log.warn("Certificate parsed from header {} is not an X.509 certificate.", certFromHeader);
                }
            } catch (CertificateException | IllegalArgumentException e) {
                apimlLog.log("org.zowe.apiml.security.common.filter.errorParsingCertificate", Optional.ofNullable(request.getRemoteAddress()).map(InetSocketAddress::getAddress).map(InetAddress::getHostAddress).orElse("N/A"), e.getMessage(), certFromHeader);
                log.debug("Certificate parsing failed.", e);
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
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Builder
    @Value
    static class SimpleSslInfo implements SslInfo {

        String sessionId;
        X509Certificate[] peerCertificates;

    }
}

