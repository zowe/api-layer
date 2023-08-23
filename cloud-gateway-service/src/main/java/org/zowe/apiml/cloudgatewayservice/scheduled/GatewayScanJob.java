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
import org.zowe.apiml.cloudgatewayservice.service.ServiceInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@EnableScheduling
@Slf4j
@Component
//Todo: properties naming, External gateway indexeder
@ConditionalOnExpression("${apiml.features.central.enabled:true}")
@RequiredArgsConstructor
public class GatewayScanJob {
    public static final String GATEWAY_SERVICE_ID = "GATEWAY";
    public static final int OUTBOUND_CALL_PARALLELISM_LEVEL = 20;

    private final GatewayIndexService gatewayIndexerService;
    private final InstanceInfoService instanceInfoService;

    @Scheduled(initialDelay = 2000, fixedDelayString = "${apiml.features.central.interval-ms:30000}")
    public void runScanExternalGateways() {
        log.debug("Scan gateways job start");
        Mono<List<ServiceInstance>> registeredGateways = instanceInfoService.getServiceInstance(GATEWAY_SERVICE_ID);
        Flux<ServiceInstance> serviceInstanceFlux = registeredGateways.flatMapMany(Flux::fromIterable);

        serviceInstanceFlux
                .parallel(OUTBOUND_CALL_PARALLELISM_LEVEL)
                .runOn(Schedulers.parallel())
                .flatMap(gatewayIndexerService::indexGatewayServices)
                .subscribe();
    }

    @Scheduled(initialDelay = 10000, fixedDelayString = "${apiml.features.central.interval-ms:15000}")
    public void dumpCaches() {
        // Debug
        gatewayIndexerService.dumpIndex();
        Map<String, List<ServiceInfo>> state = gatewayIndexerService.getCurrentState();

        log.trace("\nCurrent state:\n\t {}\n\n", state);
    }
}
