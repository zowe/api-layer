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

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;

@Service
@Slf4j
public class X509FilterFactory extends AbstractGatewayFilterFactory<X509FilterFactory.Config> {

    public static final String PUBLIC_KEY = "X-Certificate-Public";
    public static final String DISTINGUISHED_NAME = "X-Certificate-DistinguishedName";
    public static final String COMMON_NAME = "X-Certificate-CommonName";


    public X509FilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            if (exchange.getRequest().getSslInfo() != null) {
                X509Certificate[] certificates = exchange.getRequest().getSslInfo().getPeerCertificates();
                if (certificates != null && certificates.length > 0) {
                    ServerHttpRequest request = exchange.getRequest().mutate().headers(headers -> {
                        try {
                            setHeader(headers, config.getHeaders().split(","), certificates[0]);
                        } catch (CertificateEncodingException e) {
                            throw new RuntimeException(e);
                        }
                    }).build();
                    return chain.filter(exchange.mutate().request(request).build());
                }
            }
            return chain.filter(exchange);
        });
    }


    private void setHeader(HttpHeaders headers, String[] headerNames, X509Certificate certificate) throws CertificateEncodingException {
        for (String headerName : headerNames) {
            switch (headerName.trim()) {
                case COMMON_NAME:
                    headers.add(COMMON_NAME, getCommonName(certificate.getSubjectX500Principal().getName()));
                    break;
                case PUBLIC_KEY:
                    headers.add(PUBLIC_KEY, Base64.getEncoder().encodeToString(certificate.getEncoded()));
                    break;
                case DISTINGUISHED_NAME:
                    headers.add(DISTINGUISHED_NAME, certificate.getSubjectDN().getName());
                    break;
                default:
                    log.debug("Unsupported header specified in service metadata, " +
                        "please review apiml.service.authentication.headers, possible values are: " + PUBLIC_KEY +
                        ", " + DISTINGUISHED_NAME + ", " + COMMON_NAME + "\nprovided value: " + headerName);

            }
        }
    }

    private String getCommonName(String dn){
        try {
            LdapName  ldapDN = new LdapName(dn);
            for (Rdn rdn : ldapDN.getRdns()) {
                if ("cn".equalsIgnoreCase(rdn.getType())) {
                    return String.valueOf(rdn.getValue());
                }
            }
        } catch (InvalidNameException e) {
            throw new RuntimeException(e);
        }
        return null;
    }


    public static class Config {
        private String headers;

        public String getHeaders() {
            return headers;
        }

        public void setHeaders(String headers) {
            this.headers = headers;
        }
    }
}
