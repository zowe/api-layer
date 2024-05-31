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
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zowe.apiml.cloudgatewayservice.service.GatewayIndexService;
import org.zowe.apiml.cloudgatewayservice.service.InstanceInfoService;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.services.BasicInfoService;
import org.zowe.apiml.services.ServiceInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.zowe.apiml.constants.EurekaMetadataDefinition.APIML_ID;

/**
 * Scheduled job to refresh registry of all registered gateways services.
 * Behaviour of the job can  be configured by the following settings:
 * <pre>
 *   apiml:
 *     cloudGateway:
 *       cachePeriodSec: - default value 120 seconds
 *       maxSimultaneousRequests:  - default value 20
 *       clientKeystore: - default value null
 *       clientKeystorePassword: - default value null
 *       clientKeystoreType: - default PKCS12
 *       serviceRegistryEnabled: - default value false
 * </pre>
 */
@EnableScheduling
@Slf4j
@Component
@ConditionalOnExpression("${apiml.cloudGateway.registry.enabled:false}")
@RequiredArgsConstructor
public class GatewayScanJob {

    private final BasicInfoService basicInfoService;
    private final EurekaRegistration serviceRegistration;
    private final GatewayIndexService gatewayIndexerService;
    private final InstanceInfoService instanceInfoService;
    @Value("${apiml.service.apimlId:#{null}}")
    private String currentApimlId;
    @Value("${apiml.cloudGateway.maxSimultaneousRequests:20}")
    private int maxSimultaneousRequests;

    @Scheduled(initialDelay = 5000, fixedDelayString = "${apiml.cloudGateway.refresh-interval-ms:30000}")
    public void startScanExternalGatewayJob() {

        log.debug("Scan gateways job start");
        doScanExternalGateway()
                .subscribe();
        addLocalServices();
    }

    private void addLocalServices() {
        String apimlIdKey = Optional.ofNullable(currentApimlId).orElse(serviceRegistration.getInstanceId());
        List<ServiceInfo> localServices = basicInfoService.getServicesInfo();
        gatewayIndexerService.putApimlServices(apimlIdKey, localServices);
    }

    /**
     * reactive entry point  for the external gateways index refresh
     */
    protected Flux<List<ServiceInfo>> doScanExternalGateway() {
        Mono<List<ServiceInstance>> registeredGateways = instanceInfoService.getServiceInstance(CoreService.GATEWAY.getServiceId())
                .map(gateways -> gateways.stream().filter(info -> !StringUtils.equals(info.getMetadata().getOrDefault(APIML_ID, "N/A"), currentApimlId)).collect(Collectors.toList()));

        Flux<ServiceInstance> serviceInstanceFlux = registeredGateways.flatMapMany(Flux::fromIterable);

        return serviceInstanceFlux
                .flatMap(gatewayIndexerService::indexGatewayServices, maxSimultaneousRequests);
    }
}
