package org.zowe.apiml.cloudgatewayservice.scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

import java.util.List;
import java.util.Map;

/**
 * Scheduled job to refresh registry of all registered gateways services.
 * Behaviour of the job can  be configured by the following settings:
 * <pre>
 *   apiml:
 *     cloudGateway:
 *       cachePeriodSec: - default value 120 seconds
 *       parallelismLevel:  - default value 20
 *       outboundKeystore: - default value null
 *       outboundKeystorePassword: - default value null
 *       gatewayScanJobEnabled: - default value true *
 * </pre>
 */
@EnableScheduling
@Slf4j
@Component
@ConditionalOnExpression("${apiml.cloudGateway.gatewayScanJobEnabled:true}")
@RequiredArgsConstructor
public class GatewayScanJob {
    public static final String GATEWAY_SERVICE_ID = "GATEWAY";
    private final GatewayIndexService gatewayIndexerService;
    private final InstanceInfoService instanceInfoService;
    @Value("${apiml.cloudGateway.parallelismLevel:20}")
    private int parallelismLevel;

    @Scheduled(initialDelay = 2000, fixedDelayString = "${apiml.cloudGateway.refresh-interval-ms:30000}")
    public void runScanExternalGateways() {
        log.debug("Scan gateways job start");
        Mono<List<ServiceInstance>> registeredGateways = instanceInfoService.getServiceInstance(GATEWAY_SERVICE_ID);
        Flux<ServiceInstance> serviceInstanceFlux = registeredGateways.flatMapMany(Flux::fromIterable);

        serviceInstanceFlux
                .flatMap(gatewayIndexerService::indexGatewayServices, parallelismLevel)
                .subscribe();
    }

    @Scheduled(initialDelay = 10000, fixedDelayString = "5000")
    public void dumpCaches() {
        // Debug
        gatewayIndexerService.dumpIndex();
        Map<String, List<ServiceInfo>> fullState = gatewayIndexerService.listRegistry(null, null);


        log.trace("\nCurrent state:\n\t {}\n\n", fullState);
    }
}
