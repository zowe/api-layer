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

import com.netflix.appinfo.InstanceInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;
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

    private InstanceInfo findEurekaInstance(String serviceId) {
        var services = discoveryClient.getServices();

        if (StringUtils.isEmpty(serviceId) || services.stream().noneMatch(serviceId::equalsIgnoreCase)) {
            throw new InstanceNotFoundException("Service '" + serviceId + "' is not registered to Discovery Service");
        }

        var instances = discoveryClient.getInstances(serviceId);
        return instances.stream()
            .filter(EurekaServiceInstance.class::isInstance)
            .map(EurekaServiceInstance.class::cast)
            .map(EurekaServiceInstance::getInstanceInfo)
            .filter(ii -> EurekaMetadataDefinition.RegistrationType.of(ii.getMetadata()).isPrimary())
            .findFirst()
            .orElseThrow(() -> new InstanceNotFoundException("'" + serviceId + "' has no running instances registered to Discovery Service"));
    }

    /**
     * Run the lookup and provide the logic to be executed
     *
     * @param serviceId             service id being looked up
     * @param action                Consumer interface lambda to process and accept the retrieved InstanceInfo
     * @param handleFailureConsumer BiConsumer interface lambda to provide exception handling logic
     */
    public void run(String serviceId,
                    Consumer<InstanceInfo> action,
                    BiConsumer<Exception, Boolean> handleFailureConsumer) {
        log.debug("Started instance finder");

        try {
            InstanceInfo instanceInfo = findEurekaInstance(serviceId);
            log.debug("App found {}", instanceInfo.getAppName());

            action.accept(instanceInfo);
        } catch (InstanceNotFoundException | RetryException e) {
            log.debug(e.getMessage());
            handleFailureConsumer.accept(e, false);
        } catch (Exception e) {
            handleFailureConsumer.accept(e, true);
            log.debug("Unexpected exception while retrieving '{}' service from Eureka", serviceId, e);
        }

    }

}
