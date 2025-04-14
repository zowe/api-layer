package org.zowe.apiml.gateway.config;

import org.springframework.http.HttpHeaders;
import org.springframework.security.web.server.header.ServerHttpHeadersWriter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.time.Duration;

public class CustomHstsServerHttpHeadersWriter implements ServerHttpHeadersWriter {

    private static final String DEFAULT_MAX_AGE = "max-age=" + Duration.ofDays(365L).getSeconds();
    private static final String DEFAULT_INCLUDE_SUBDOMAINS = "; includeSubDomains";
    private final String headerValue = DEFAULT_MAX_AGE + DEFAULT_INCLUDE_SUBDOMAINS;

    @Override
    public Mono<Void> writeHttpHeaders(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getResponse().getHeaders();
        headers.set("Strict-Transport-Security", headerValue);
        return Mono.empty();
    }
}
