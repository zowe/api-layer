package org.zowe.apiml.gateway.filters;

import org.springframework.http.HttpCookie;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Component
public class KeyResolver implements org.springframework.cloud.gateway.filter.ratelimit.KeyResolver {

    private final String cookieName;

    public KeyResolver() {
        this.cookieName = "apimlAuthenticationToken";
    }

    @Override
    public Mono<String> resolve(org.springframework.web.server.ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getCookies().getOrDefault(cookieName, Collections.emptyList())
            .stream()
            .findFirst()
            .map(HttpCookie::getValue)
            .orElse(null)
        );
    }
}
