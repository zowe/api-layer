package org.zowe.apiml.filter.otlp;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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
@Slf4j
public class ServiceIdMetricsFilter implements GlobalFilter, Ordered {

    private final DoubleHistogram durationHistogram;
    private final Logger otelLogger;


    public ServiceIdMetricsFilter(OpenTelemetry openTelemetry) { // Inject the meter bean to leverage the opentelemetry instrumentation
        var meter = openTelemetry.getMeter("zowe-apiml");  //TODO constant
        this.durationHistogram = meter
            .histogramBuilder("apiml.service.request.duration") //TODO: could be just count
            .setDescription("Gateway request duration per serviceId")
            .setUnit("s")
            .build();

        otelLogger = openTelemetry.getLogsBridge().get("zowe-apiml");

        //TODO: should be part of some applicatio configuration instead of here. although the effect is the same
        // Automatically propagates context and MDC across threads
        //Not a good idea - MDC context is globas while reactor context is request-specific
        // The reacto context must be the single source of truth
        // The way could be restoring MDC context just before logging, but be careful, it must happen within the same signal (beware of asyc operators and inner publishers)
        //Must be registered early enough
        //Hooks.enableAutomaticContextPropagation();

    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //TODO consider use of atomic reference and initialize in onSubscribe
        long start = System.nanoTime();

        MDC.put("initial", "yes");

//        Mono.defer(() -> {
//                log.info("Stage 1 MDC: {}", MDC.getCopyOfContextMap());
//                MDC.put("stage", "1");
//                return Mono.just("data");
//            })
//            .publishOn(Schedulers.boundedElastic())
//            .doOnNext(v -> {
//                log.info("Stage 2 MDC: {}", MDC.getCopyOfContextMap());
//            })
//            .block();

        MDC.put("apiml.service.foo", "FOOOOOO");
        log.warn("serviceId in the log FOOOOOO");

        return chain.filter(exchange)
            //.contextWrite(Context.of("mdc-context", MDC.getCopyOfContextMap()))
            .transformDeferredContextual( (mono, ctx) -> {
                log.warn("Thread: {}, MDC: {}", Thread.currentThread().getName(), MDC.getCopyOfContextMap());
                log.warn("Thread: {}, ctx: {}", Thread.currentThread().getName(), ctx);
                return mono;
            })
//            .doOnSubscribe( __ -> {
//                log.warn("Thread: {}, MDC: {}", Thread.currentThread().getName(), MDC.getCopyOfContextMap());
//                MDC.put("apiml.service.bar", "BAAAAAAR");
//                log.warn("serviceId in the log BAAAAAAR");
//
//            })
//            .transformDeferredContextual( (mono, ctx) -> {
//                log.warn("Thread: {}, MDC: {}", Thread.currentThread().getName(), MDC.getCopyOfContextMap());
//                log.warn("Thread: {}, ctx: {}", Thread.currentThread().getName(), ctx);
//                return mono;
//            })
            .doOnNext(__ -> log.warn("onNext: Thread: {}, MDC: {}", Thread.currentThread().getName(), MDC.getCopyOfContextMap()))
            .doOnSuccess( __ -> log.warn("onSuccess: Thread: {}, MDC: {}", Thread.currentThread().getName(), MDC.getCopyOfContextMap()))
            .doOnError( __ -> log.warn("onError: Thread: {}, MDC: {}", Thread.currentThread().getName(), MDC.getCopyOfContextMap()))
            .doFinally(signalType -> {
                log.warn("Thread: {}, MDC: {}", Thread.currentThread().getName(), MDC.getCopyOfContextMap());
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

                    //add attribute to MDC fro logging
                    //check the log when the field are populated based on when they are set
                    //TODO: MDC is not reliably carried over the threads
                    //Consider writing a turbofilter to access specific spat attribute
                    //Or using reactor.core.publisher.Hooks.enableAutomaticContextPropagation()
                    //Be sure the context is cleared so the attributes does not propagate to unrelated messages
                    MDC.put("apiml.service.id", serviceId);
                    log.warn("serviceId in the log {}", serviceId);
                    log.warn("Thread: {}, MDC: {}", Thread.currentThread().getName(), MDC.getCopyOfContextMap());

                    otelLogger.logRecordBuilder()
                        .setSeverity(Severity.WARN)
                        .setAttribute(io.opentelemetry.api.common.AttributeKey.stringKey("apiml.service.id"), serviceId)
                        .setAttribute(io.opentelemetry.api.common.AttributeKey.stringKey("apiml.user.id"), List.of("Bob", "Alice", "Marty").get(new Random().nextInt(2)))
                        .setBody("Request processed")
                        .emit();
                }
            })
            .transformDeferredContextual( (mono, ctx) -> {
                log.warn("Thread: {}, MDC: {}", Thread.currentThread().getName(), MDC.getCopyOfContextMap());
                log.warn("Thread: {}, ctx: {}", Thread.currentThread().getName(), ctx);
                return mono;
            });
    }

    @Override
    public int getOrder() {
        //TODO tune the order
        return 0;
    }
}
