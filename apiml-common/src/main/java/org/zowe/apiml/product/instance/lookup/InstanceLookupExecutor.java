/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.instance.lookup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.ServiceInstance;
import org.zowe.apiml.constants.EurekaMetadataDefinition;
import org.zowe.apiml.product.instance.InstanceNotFoundException;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Generic executor that searches the EurekaClient for specific instance
 */
@Slf4j
@RequiredArgsConstructor
public class InstanceLookupExecutor {

    private final DiscoveryClient discoveryClient;

    /**
     * Finds the primary registration of a service.
     * <p>
     * Works against Spring Cloud's own {@link ServiceInstance} rather than downcasting to
     * {@code EurekaServiceInstance} to reach a Netflix {@code InstanceInfo}. The cast was fragile as well as
     * Netflix-specific: any {@code DiscoveryClient} that does not hand back that exact wrapper - including the
     * replacement registry's - silently matched nothing and the lookup failed with "no running instances".
     * Everything this method actually needs is the metadata, which is on the Spring interface.
     */
    private ServiceInstance findPrimaryInstance(String serviceId) {
        var services = discoveryClient.getServices();

        if (StringUtils.isEmpty(serviceId) || services.stream().noneMatch(serviceId::equalsIgnoreCase)) {
            throw new InstanceNotFoundException("Service '" + serviceId + "' is not registered to Discovery Service");
        }

        var instances = discoveryClient.getInstances(serviceId);
        return instances.stream()
            .filter(instance -> EurekaMetadataDefinition.RegistrationType.of(instance.getMetadata()).isPrimary())
            .findFirst()
            .orElseThrow(() -> new InstanceNotFoundException("'" + serviceId + "' has no running instances registered to Discovery Service"));
    }

    /**
     * Run the lookup and provide the logic to be executed
     *
     * @param serviceId             service id being looked up
     * @param action                Consumer interface lambda to process the discovered service instance
     * @param handleFailureConsumer BiConsumer interface lambda to provide exception handling logic
     */
    public void run(String serviceId,
                    Consumer<ServiceInstance> action,
                    BiConsumer<Exception, Boolean> handleFailureConsumer) {
        log.debug("Started instance finder");

        try {
            ServiceInstance instance = findPrimaryInstance(serviceId);
            log.debug("App found {}", instance.getServiceId());

            action.accept(instance);
        } catch (InstanceNotFoundException | RetryException e) {
            log.debug(e.getMessage());
            handleFailureConsumer.accept(e, false);
        } catch (Exception e) {
            handleFailureConsumer.accept(e, true);
            log.debug("Unexpected exception while retrieving '{}' service from Eureka", serviceId, e);
        }

    }

}
