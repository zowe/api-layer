/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.filters;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.zowe.apiml.cloudgatewayservice.security.AuthSourceSign;
import org.zowe.apiml.constants.ApimlConstants;

import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * Objective is to include new headers in the request which contain authentication source (client cert)
 * and its signature for further processing by the domain gateway.
 */
@Service
@Slf4j
public class AuthSourceFilterFactory extends AbstractGatewayFilterFactory<AuthSourceFilterFactory.Config> {

    public AuthSourceFilterFactory(AuthSourceSign authSourceSign) {
        super(Config.class);
        this.authSourceSign = authSourceSign;
    }

    private final AuthSourceSign authSourceSign;

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            if (exchange.getRequest().getSslInfo() != null) {
                X509Certificate[] certificates = exchange.getRequest().getSslInfo().getPeerCertificates();
                if (certificates != null && certificates.length > 0) {
                    ServerHttpRequest request = exchange.getRequest().mutate().headers(headers -> {
                        try {
                            byte[] certBytes = certificates[0].getEncoded();
                            byte[] signBytes = authSourceSign.sign(certBytes);
                            final String authSource = Base64.getEncoder().encodeToString(certBytes);
                            final String authSignature = Base64.getEncoder().encodeToString(signBytes);
                            headers.add(config.getAuthSourceHeader(), authSource);
                            headers.add(config.getAuthSignatureHeader(), authSignature);
                        } catch (CertificateEncodingException e) {
                            headers.add(ApimlConstants.AUTH_FAIL_HEADER, "Invalid client certificate in request. Error message: " + e.getMessage());
                        } catch (SignatureException e) {
                            headers.add(ApimlConstants.AUTH_FAIL_HEADER, "Failed to sign the client certificate in request. Error message: " + e.getMessage());
                        }
                    }).build();
                    return chain.filter(exchange.mutate().request(request).build());
                }
            }
            return chain.filter(exchange);
        });
    }

    @Getter
    @Setter
    public static class Config {
        private String authSourceHeader;
        private String authSignatureHeader;
    }
}
