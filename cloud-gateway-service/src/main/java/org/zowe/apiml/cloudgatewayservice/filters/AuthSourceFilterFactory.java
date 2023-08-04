package org.zowe.apiml.cloudgatewayservice.filters;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.zowe.apiml.message.core.MessageService;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * Objective is to include new header in the request which contain authentication source (client cert)
 * for further processing by the domain gateway.
 */
@Service
@Slf4j
public class AuthSourceFilterFactory extends AbstractGatewayFilterFactory<AuthSourceFilterFactory.Config> {

    public AuthSourceFilterFactory(MessageService messageService) {
        super(Config.class);
        this.messageService = messageService;
    }

    private final MessageService messageService;

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            String authSource = "none";
            if (exchange.getRequest().getSslInfo() != null) {
                X509Certificate[] certificates = exchange.getRequest().getSslInfo().getPeerCertificates();
                if (certificates != null && certificates.length > 0) {
                    byte[] certBytes = new byte[0];
                    try {
                        certBytes = certificates[0].getEncoded();
                    } catch (CertificateEncodingException e) {
                        throw new RuntimeException(e);
                    }
                    authSource = Base64.getEncoder().encodeToString(certBytes);
                }
            }
            String finalAuthSource = authSource;
            ServerHttpRequest request = exchange.getRequest().mutate().headers(headers -> {
                headers.add(config.getHeaderName(), finalAuthSource);

            }).build();
            return chain.filter(exchange.mutate().request(request).build());
        });
    }

    @Getter
    @Setter
    public static class Config {
        private String headerName;
    }
}
