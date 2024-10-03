package org.zowe.apiml.gateway.filters;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InMemoryRateLimiterFilterFactory extends AbstractGatewayFilterFactory<InMemoryRateLimiterFilterFactory.Config> {

    private final InMemoryRateLimiter rateLimiter;
    private final KeyResolver keyResolver;
    @Value(value = "${apiml.routing.servicesToLimitRequestRate}")
    List<String> serviceIds;

    public InMemoryRateLimiterFilterFactory(InMemoryRateLimiter rateLimiter, KeyResolver keyResolver) {
        super(Config.class);
        this.rateLimiter = rateLimiter;
        this.keyResolver = keyResolver;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String requestPath = exchange.getRequest().getPath().elements().get(1).value();
            if (serviceIds.contains(requestPath)) {
                return keyResolver.resolve(exchange).flatMap(key -> {
                    return rateLimiter.isAllowed(config.getRouteId(), key).flatMap(response -> {
                        if (response.isAllowed()) {
                            return chain.filter(exchange);
                        } else {
                            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                            return exchange.getResponse().setComplete();
                        }
                    });
                });
            } else {
                return chain.filter(exchange);
            }
        };
    }

    @Getter
    @Setter
    public static class Config {
        private String routeId;
        private Integer capacity;
        private Integer tokens;
        private Integer refillIntervalSeconds;
    }
}
