/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.x509;

import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.stereotype.Service;
import org.zowe.apiml.security.common.verify.CertificateValidator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

import static org.zowe.apiml.gateway.x509.ForwardClientCertFilterFactory.CLIENT_CERT_HEADER;

@Slf4j
@Service
public class AcceptForwardedClientCertFilterFactory extends AbstractGatewayFilterFactory<AcceptForwardedClientCertFilterFactory.Config> {

    private final CertificateValidator certificateValidator;

    public AcceptForwardedClientCertFilterFactory(CertificateValidator certificateValidator) {
        super(Config.class);
        this.certificateValidator = certificateValidator;
    }

    private X509Certificate[] getClientCertificateFromHeader(ServerHttpRequest request) {
        String forwarderCert = request.getHeaders().getFirst(CLIENT_CERT_HEADER);
        if (forwarderCert == null) return new X509Certificate[0];

        byte[] encodedCertificate = Base64.getDecoder().decode(forwarderCert);
        try (var bais = new ByteArrayInputStream(encodedCertificate)) {
            var certificate = (X509Certificate) CertificateFactory
                .getInstance("X.509")
                .generateCertificate(bais);
            return new X509Certificate[]{certificate};
        } catch (CertificateException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public GatewayFilter apply(AcceptForwardedClientCertFilterFactory.Config config) {
        return (exchange, chain) -> {
            SslInfo sslInfo = exchange.getRequest().getSslInfo();
            X509Certificate[] x509Certificates = sslInfo == null ? null : sslInfo.getPeerCertificates();
            if ((x509Certificates != null) && (x509Certificates.length > 0) && certificateValidator.isTrusted(x509Certificates)) {
                X509Certificate[] forwardedClientCertificate = getClientCertificateFromHeader(exchange.getRequest());
                if (forwardedClientCertificate.length > 0) {
                    log.debug("Accepting forwarded client certificate {}", forwardedClientCertificate[0].getSubjectX500Principal().getName());
                    var request = exchange.getRequest().mutate().sslInfo(CustomSslInfo.builder()
                        .peerCertificates(forwardedClientCertificate)
                        .build()
                    ).build();
                    return chain.filter(exchange.mutate().request(request).build());
                }
            }
            return chain.filter(exchange);
        };
    }


    @SuppressWarnings("squid:S2094")
    public static class Config {
    }

    @Builder
    @Value
    static class CustomSslInfo implements SslInfo {

        private String sessionId;
        private X509Certificate[] peerCertificates;

    }

}
