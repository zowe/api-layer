/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

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

/**
 * Scheduled job to refresh registry of all registered gateways services.
 * Behaviour of the job can  be configured by the following settings:
 * <pre>
 *   apiml:
 *     cloudGateway:
 *       cachePeriodSec: - default value 120 seconds
 *       parallelismLevel:  - default value 20
 *       clientKeystore: - default value null
 *       clientKeystorePassword: - default value null
 *       gatewayScanJobEnabled: - default value true
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

    @Scheduled(initialDelay = 5000, fixedDelayString = "${apiml.cloudGateway.refresh-interval-ms:30000}")
    public void startScanExternalGatewayJob() {
        log.debug("Scan gateways job start");
        doScanExternalGateway()
                .subscribe();
    }

    /**
     * reactive entry point  for the external gateways index refresh
     */
    protected Flux<List<ServiceInfo>> doScanExternalGateway() {

        Mono<List<ServiceInstance>> registeredGateways = instanceInfoService.getServiceInstance(GATEWAY_SERVICE_ID);
        Flux<ServiceInstance> serviceInstanceFlux = registeredGateways.flatMapMany(Flux::fromIterable);

        return serviceInstanceFlux
                .flatMap(gatewayIndexerService::indexGatewayServices, parallelismLevel);
    }

}
