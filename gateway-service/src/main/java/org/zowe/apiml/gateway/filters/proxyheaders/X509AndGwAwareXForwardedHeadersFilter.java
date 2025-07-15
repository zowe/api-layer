/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters.proxyheaders;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.gateway.filter.headers.XForwardedHeadersFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.security.HttpsConfig;
import org.zowe.apiml.security.SecurityUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * The aim of the class is to ensure that the content of X-Forwarded-* or Forwarded headers contains only trusted data.
 * Forged values of these headers can pose a security risk as identified by CVE-2025-41235.
 * Trusted proxies can be defined via config property `apiml.security.forwardHeader.trustedProxies` that is equivalent of
 * the Spring's original one `spring.cloud.gateway.mvc.trustedProxies`. The benefit of this bean is that it is not
 * necessary to validate headers if the request is signed by trusted Gateway certificate. It solves, for example,
 * the case when multiple APIML Gateways routes each other. The http context cannot be compromised when the request
 * is signed by a trusted certificate, so the content of headers is considered valid, and it is not necessary to verify
 * the host against the list. Otherwise, if the request is not signed, the client address should be validated against configuration.
 * <p>
 * The implementation supports empty configuration value. The empty value means when the request is signed, the
 * content of headers is accepted otherwise the headers are considered vulnerable and are removed from the request.
 * <p>
 * Signed / defined pattern | empty list of trusted proxies | a trusted proxy is defined
 * -------------------------+-------------------------------+---------------------------
 * untrusted signature/no   |        headers removed        |   check against the list
 * -------------------------+-------------------------------+---------------------------
 * trusted signature        |        forward headers        |      forward headers
 */

@Slf4j
public class X509AndGwAwareXForwardedHeadersFilter extends XForwardedHeadersFilter {

    // Generic all-in-one Forwarded header not handled by the default spring filter
    public static final String FORWARDED_HEADER = "Forwarded";

    final Set<String> certificateChainBase64;
    final Predicate<String> isProxyTrusted;
    final String trustedProxiesRegex;
    final AtomicReference<Set<String>> trustedAdditionalGateways;

    /*
     *
     * @param httpsConfig gateway certificate configuration
     * @param trustedProxiesPattern configuration value of a pattern on how validate proxy
     * @param additionalRegistrationGatewayRegistry cache of apiml gateway ip addresses from additional registrations
     *
     */
    public X509AndGwAwareXForwardedHeadersFilter(
        HttpsConfig httpsConfig,
        String trustedProxiesPattern,
        AdditionalRegistrationGatewayRegistry additionalRegistrationGatewayRegistry)
        throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        // Trustworthiness of a proxy is evaluated by this class,
        // hence the spring filter must trust everything and not interfere
        super(".*");
        certificateChainBase64 = SecurityUtils.loadCertificateChainBase64(httpsConfig);
        trustedProxiesRegex = trustedProxiesPattern;
        trustedAdditionalGateways = additionalRegistrationGatewayRegistry.getAdditionalGatewayIpAddressesReference();

        Predicate<String> isTrusted = host -> trustedAdditionalGateways.get().contains(host);
        if (StringUtils.isEmpty(trustedProxiesRegex)) {
            isTrusted = isTrusted.or(host -> false);
        } else {
            Pattern pattern = Pattern.compile(trustedProxiesRegex);
            isTrusted = isTrusted.or(host -> host != null && pattern.matcher(host).matches());
        }
        this.isProxyTrusted = isTrusted;
    }

    @Override
    public HttpHeaders filter(HttpHeaders input, ServerWebExchange exchange) {
        if (!hasXForwardedHeader(input)) return super.filter(input, exchange);

        boolean trustedSourceByX509 = Optional.ofNullable(exchange.getRequest().getSslInfo())
            .map(SslInfo::getPeerCertificates)
            .filter(certs -> certs.length > 0)
            .map(certs -> Arrays.stream(certs)
                .map(SecurityUtils::base64EncodePublicKey)
                .allMatch(certificateChainBase64::contains)
            )
            .orElse(false);

        if (!trustedSourceByX509) {
            ServerHttpRequest request = exchange.getRequest();
            InetSocketAddress remoteAddress = request.getRemoteAddress();
            if (remoteAddress == null) {
                log.debug("Remote address is null and cannot be evaluated for trusted proxy.");
                return super.filter(removeXForwardHttpHeaders(input), exchange);
            }
            if (!isProxyTrusted.test(remoteAddress.getHostString())) {
                //Mask the address if it is not trusted so it cannot be used to build the forward headers
                ServerWebExchange sanitizedExchange = exchange.mutate().request(
                    new ServerHttpRequestDecorator(request) {
                        @Override
                        public InetSocketAddress getRemoteAddress() {
                            return null;
                        }
                    }
                ).build();
                log.debug("Remote address not trusted. Trusted proxies pattern: {}, remote address: {}", trustedProxiesRegex, remoteAddress);
                return super.filter(removeXForwardHttpHeaders(input), sanitizedExchange);
            }
        }
        return super.filter(input, exchange);
    }

    private HttpHeaders removeXForwardHttpHeaders(HttpHeaders input) {
        HttpHeaders h = new HttpHeaders();
        input.forEach((header, values) -> {
            if (!isXForwardedHeader(header)) {
                h.put(header, values);
            }
        });
        return h;
    }

    private boolean isXForwardedHeader(String header) {
        return header.equalsIgnoreCase(X_FORWARDED_FOR_HEADER) ||
            header.equalsIgnoreCase(X_FORWARDED_HOST_HEADER) ||
            header.equalsIgnoreCase(X_FORWARDED_PORT_HEADER) ||
            header.equalsIgnoreCase(X_FORWARDED_PROTO_HEADER) ||
            header.equalsIgnoreCase(X_FORWARDED_PREFIX_HEADER) ||
            header.equalsIgnoreCase(FORWARDED_HEADER);
    }

    private boolean hasXForwardedHeader(HttpHeaders headers) {
        return headers.keySet().stream()
            .anyMatch(this::isXForwardedHeader);
    }
}
