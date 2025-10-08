package org.zowe.apiml.filter.otlp;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.trace.Span;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Random;

@Component
public class ServiceIdMetricsFilter implements GlobalFilter, Ordered {

    private final DoubleHistogram durationHistogram;

    public ServiceIdMetricsFilter(OpenTelemetry openTelemetry) { // Inject the meter bean to leverage the opentelemetry instrumentation
        var meter = openTelemetry.getMeter("zowe-apiml");  //TODO constant
        this.durationHistogram = meter
            .histogramBuilder("apiml.service.request.duration") //TODO: could be just count
            .setDescription("Gateway request duration per serviceId")
            .setUnit("s")
            .build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //TODO consider use of atomic reference and initialize in onSubscribe
        long start = System.nanoTime();

        return chain.filter(exchange)
            .doFinally(signalType -> {
                Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
                if (route != null) {
                    String serviceId = route.getUri().getHost();
                    long end = System.nanoTime();
                    double durationSeconds = (end - start) / 1_000_000_000.0;
                    durationHistogram.record(
                        durationSeconds,
                        io.opentelemetry.api.common.Attributes.of(
                            io.opentelemetry.api.common.AttributeKey.stringKey("apiml.service.id"), serviceId,
                            io.opentelemetry.api.common.AttributeKey.stringKey("apiml.user.id"), List.of("Bob", "Alice", "Marty").get(new Random().nextInt(2)))
                        //TODO: in reality we will need this one set after authentication
                        // We should be able to move the filter after authentication
                        // - the time needed to identify the route inside scg is not included
                        // - the start happens during the filter chain assembly, so we can measure the time as it is now
                        // - alternatively, we can move the time measurement to on subscribe
                        // - GW endpoints and 404 for non-existent services are not included (WebFilter can be used as alternative, but it will include also non-routed GW endpoints)
                        // - can be just a counter
                    );

                    //Attributes added to tracing
                    var span = Span.current();
                    span.setAttribute(io.opentelemetry.api.common.AttributeKey.stringKey("apiml.service.id"), serviceId);
                    span.setAttribute(io.opentelemetry.api.common.AttributeKey.stringKey("apiml.user.id"), List.of("Bob", "Alice", "Marty").get(new Random().nextInt(2)));
                }
            });
    }

    @Override
    public int getOrder() {
        //TODO tune the order
        return 0;
    }
}
