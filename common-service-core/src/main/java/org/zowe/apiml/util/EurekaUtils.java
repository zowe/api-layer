/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util;

import com.netflix.appinfo.InstanceInfo;
import lombok.experimental.UtilityClass;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.zowe.apiml.constants.EurekaMetadataDefinition;

import java.util.Optional;

import static org.zowe.apiml.constants.EurekaMetadataDefinition.APIML_ID;
import static org.zowe.apiml.product.constants.CoreService.GATEWAY;

/**
 * This util offer basic operation with eureka, like: extraction serviceId from instanceId, construct URL by
 * InstanceInfo etc.
 */
@UtilityClass
public class EurekaUtils {

    /**
     * Extract serviceId from instanceId
     * @param instanceId input, instanceId in format "host:service:random number to unique instanceId"
     * @return second part, it means serviceId. If it doesn't exist return null;
     */
    public String getServiceIdFromInstanceId(String instanceId) {
        final int startIndex = instanceId.indexOf(':');
        if (startIndex < 0) return null;

        final int endIndex = instanceId.indexOf(':', startIndex + 1);
        if (endIndex < 0) return null;

        return instanceId.substring(startIndex + 1, endIndex);
    }

    /**
     * Construct base URL for specific InstanceInfo
     * @param instanceInfo Instance of service, for which we want to get an URL
     * @return URL to the instance
     */
    public String getUrl(InstanceInfo instanceInfo) {
        if (instanceInfo.getSecurePort() == 0 || !instanceInfo.isPortEnabled(InstanceInfo.PortType.SECURE)) {
            return "http://" + instanceInfo.getHostName() + ":" + instanceInfo.getPort();
        } else {
            return "https://" + instanceInfo.getHostName() + ":" + instanceInfo.getSecurePort();
        }
    }

    private Optional<ServiceInstance> getPrimaryInstanceInfo(DiscoveryClient discoveryClient, String serviceId) {
        return Optional.ofNullable(discoveryClient.getInstances(serviceId))
            .map(instances -> instances.stream()
                .filter(instance -> EurekaMetadataDefinition.RegistrationType.of(instance.getMetadata()).isPrimary())
                .findFirst()
                .orElse(null)
            );
    }

    private Optional<ServiceInstance> getSecondaryInstanceInfo(DiscoveryClient discoveryClient, String apimlId) {
        return Optional.ofNullable(discoveryClient.getInstances(GATEWAY.getServiceId()))
            .map(instances -> instances.stream()
                .filter(instance -> EurekaMetadataDefinition.RegistrationType.of(instance.getMetadata()).isAdditional())
                .filter(instance -> apimlId.equals(instance.getMetadata().get(APIML_ID)))
                .findFirst()
                .orElse(null)
            );
    }

    /**
     * It tries to find service with primary registration, if it does not exist it looks also
     * for a gateway with secondary registration and id is used as apimlId
     * @param discoveryClient eureka client instance for look up
     * @param id serviceId for primary or apimlId for secondary registration
     * @return instance or empty Optional object
     */
    public Optional<ServiceInstance> getInstanceInfo(DiscoveryClient discoveryClient, String id) {
        if (id == null) {
            return Optional.empty();
        }
        return getPrimaryInstanceInfo(discoveryClient, id)
            .or(() -> getSecondaryInstanceInfo(discoveryClient, id));
    }

}
