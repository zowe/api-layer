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
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.message.core.MessageService;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;

import static org.zowe.apiml.constants.ApimlConstants.HTTP_CLIENT_USE_CLIENT_CERTIFICATE;

@Service
@Slf4j
public class X509FilterFactory extends AbstractGatewayFilterFactory<X509FilterFactory.Config> {

    public static final String PUBLIC_KEY = "X-Certificate-Public";
    public static final String DISTINGUISHED_NAME = "X-Certificate-DistinguishedName";
    public static final String COMMON_NAME = "X-Certificate-CommonName";

    private final MessageService messageService;


    public X509FilterFactory(MessageService messageService) {
        super(Config.class);
        this.messageService = messageService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            if (exchange.getRequest().getSslInfo() != null) {
                X509Certificate[] certificates = exchange.getRequest().getSslInfo().getPeerCertificates();
                if (certificates != null && certificates.length > 0) {
                    ServerHttpRequest request = exchange.getRequest().mutate().headers(headers -> {
                        try {
                            exchange.getAttributes().put(HTTP_CLIENT_USE_CLIENT_CERTIFICATE, Boolean.TRUE);
                            setHeader(headers, config.getHeaders().split(","), certificates[0]);
                        } catch (CertificateEncodingException | InvalidNameException e) {
                            headers.add(ApimlConstants.AUTH_FAIL_HEADER, "Invalid client certificate in request. Error message: " + e.getMessage());
                        }
                    }).build();
                    return chain.filter(exchange.mutate().request(request).build());
                }
            }
            return chain.filter(exchange.mutate().request(updateHeadersForError(exchange)).build());
        });
    }

    private ServerHttpRequest updateHeadersForError(ServerWebExchange exchange) {
        String headerValue = messageService.createMessage("org.zowe.apiml.gateway.security.schema.missingX509Authentication").mapToLogMessage();
        ServerHttpRequest request = exchange.getRequest().mutate().header(ApimlConstants.AUTH_FAIL_HEADER, headerValue).build();
        exchange.getResponse().getHeaders().add(ApimlConstants.AUTH_FAIL_HEADER, headerValue);
        return request;
    }


    private void setHeader(HttpHeaders headers, String[] headerNames, X509Certificate certificate) throws CertificateEncodingException, InvalidNameException {
        for (String headerName : headerNames) {
            switch (headerName.trim()) {
                case COMMON_NAME:
                    headers.add(COMMON_NAME, getCommonName(new LdapName(certificate.getSubjectDN().getName())));
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

    public static String getCommonName(LdapName ldapDN) {
        for (Rdn rdn : ldapDN.getRdns()) {
            if ("cn".equalsIgnoreCase(rdn.getType())) {
                return String.valueOf(rdn.getValue());
            }
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
