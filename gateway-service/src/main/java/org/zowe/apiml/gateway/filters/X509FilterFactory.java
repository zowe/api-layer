/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.product.opentelemetry.OtelRequestContext;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

import static org.zowe.apiml.constants.ApimlConstants.HTTP_CLIENT_USE_CLIENT_CERTIFICATE;

@Service
@Slf4j
public class X509FilterFactory extends AbstractGatewayFilterFactory<X509FilterFactory.Config> {

    public static final String PUBLIC_KEY = "X-Certificate-Public";
    public static final String DISTINGUISHED_NAME = "X-Certificate-DistinguishedName";
    public static final String COMMON_NAME = "X-Certificate-CommonName";

    private final MessageService messageService;

    @Value("${apiml.security.strictSchemeEnforcement:false}")
    private boolean strictSchemeEnforcement;


    public X509FilterFactory(MessageService messageService) {
        super(Config.class);
        this.messageService = messageService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            var otelRequestContext = OtelRequestContext.of(exchange);
            otelRequestContext.authMethod(AuthenticationScheme.X509);

            if (exchange.getRequest().getSslInfo() != null) {
                X509Certificate[] certificates = exchange.getRequest().getSslInfo().getPeerCertificates();
                if (certificates != null && certificates.length > 0) {
                    otelRequestContext.authSourceType("CLIENT_CERT");
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
        ServerHttpRequest request = exchange.getRequest().mutate().headers(headers -> {
            // Strict scheme enforcement: strip Authorization: Basic
            if (strictSchemeEnforcement) {
                List<String> authValues = headers.get(HttpHeaders.AUTHORIZATION);
                if (authValues != null) {
                    boolean hasBasic = authValues.stream()
                        .anyMatch(v -> v != null && v.regionMatches(true, 0, "Basic ", 0, 6));
                    if (hasBasic) {
                        headers.remove(HttpHeaders.AUTHORIZATION);
                        log.debug("Strict scheme enforcement: stripped Authorization: Basic for service (scheme: x509)");
                    }
                }
            }
            headers.add(ApimlConstants.AUTH_FAIL_HEADER, headerValue);
            exchange.getResponse().getHeaders().add(ApimlConstants.AUTH_FAIL_HEADER, headerValue);
        }).build();
        return request;
    }


    public void setHeader(HttpHeaders headers, String[] headerNames, X509Certificate certificate) throws CertificateEncodingException, InvalidNameException {
        for (String headerName : headerNames) {
            switch (headerName.trim()) {
                case COMMON_NAME:
                    headers.add(COMMON_NAME, getCommonName(new LdapName(certificate.getSubjectX500Principal().getName())));
                    break;
                case PUBLIC_KEY:
                    headers.add(PUBLIC_KEY, Base64.getEncoder().encodeToString(certificate.getEncoded()));
                    break;
                case DISTINGUISHED_NAME:
                    headers.add(DISTINGUISHED_NAME, certificate.getSubjectX500Principal().getName());
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
