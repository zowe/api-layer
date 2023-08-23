package org.zowe.apiml.cloudgatewayservice.scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zowe.apiml.cloudgatewayservice.service.GatewayIndexService;
import org.zowe.apiml.cloudgatewayservice.service.InstanceInfoService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;

@EnableScheduling
@Slf4j
@Component
//Todo: properties naming, External gateway indexeder
@ConditionalOnExpression("${apiml.features.central.enabled:true}")
@RequiredArgsConstructor
public class GatewayScanJob {

    public static final Duration GATEWAY_CALL_TIMEOUT = Duration.ofSeconds(30);
    public static final String GATEWAY_SERVICE_ID = "GATEWAY";

    private final GatewayIndexService gatewayIndexerService;
    private final InstanceInfoService instanceInfoService;

    @Scheduled(initialDelay = 2000, fixedDelayString = "${apiml.features.central.interval-ms:30000}")
    public void runScanExternalGateways() {
        log.debug("Scan gateways job start");
        Mono<List<ServiceInstance>> registeredGateways = instanceInfoService.getServiceInstance(GATEWAY_SERVICE_ID);
        Flux<ServiceInstance> serviceInstanceFlux = registeredGateways.flatMapMany(Flux::fromIterable);

        serviceInstanceFlux
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(gatewayIndexerService::indexGatewayServices)
                .subscribe();

        //.doOnNext(gatewayIndexerService::indexGatewayServices)

        //.doOnError(ex -> log.warn("Exception during gw index", ex)))
        //.doFinally(signal -> log.debug("Scan gateways job finished: {}", signal))

    }

    @Scheduled(initialDelay = 10000, fixedDelayString = "${apiml.features.central.interval-ms:15000}")
    public void dumpCaches() {
        // Debug
        gatewayIndexerService.dumpIndex();
    }
}