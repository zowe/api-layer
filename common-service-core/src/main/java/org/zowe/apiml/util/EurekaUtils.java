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
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.zowe.apiml.constants.EurekaMetadataDefinition;
import org.zowe.apiml.exception.MetadataValidationException;

import java.util.Optional;
import java.util.regex.Pattern;

import static org.zowe.apiml.constants.EurekaMetadataDefinition.APIML_ID;
import static org.zowe.apiml.product.constants.CoreService.GATEWAY;

/**
 * This util offer basic operation with eureka, like: extraction serviceId from instanceId, construct URL by
 * InstanceInfo etc.
 */
@UtilityClass
public class EurekaUtils {

    public static final Pattern SERVICE_ID_PATTERN = Pattern.compile("^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?$");

    /**
     * Extract serviceId from instanceId.
     * The instanceId format is "hostname:serviceId:port".
     * For IPv6 addresses (which contain colons), the hostname may be bracketed like "[2001:db8::1]".
     * @param instanceId input, instanceId in format "host:service:port" or "[ipv6]:service:port"
     * @return the serviceId part. If it doesn't exist or format is invalid, return null.
     */
    public String getServiceIdFromInstanceId(String instanceId) {
        if (StringUtils.isBlank(instanceId)) {
            return null;
        }

        if (instanceId.startsWith("[")) {
            int closingBracket = instanceId.indexOf("]");
            if (closingBracket > 0 && closingBracket < instanceId.length() - 1) {
                // After the closing bracket, we expect :serviceId:port
                String afterBracket = instanceId.substring(closingBracket + 1);
                if (afterBracket.startsWith(":")) {
                    String[] remainingParts = afterBracket.substring(1).split(":");
                    if (remainingParts.length == 2) {
                        String serviceId = remainingParts[0].trim();
                        return serviceId.isEmpty() ? null : serviceId;
                    }
                }
            }
            return null;
        }

        // For non-bracketed addresses, we need to handle both:
        // 1. hostname:serviceId:port (3 parts)
        // 2. ipv6Address:serviceId:port (many colons from IPv6)
        // Parse from the end: last part is port, second-to-last is serviceId
        int lastColon = instanceId.lastIndexOf(':');
        if (lastColon <= 0) {
            return null;
        }

        // Validate that port part is not empty
        String portPart = instanceId.substring(lastColon + 1).trim();
        if (portPart.isEmpty()) {
            return null;
        }

        String beforeLastColon = instanceId.substring(0, lastColon);
        int secondLastColon = beforeLastColon.lastIndexOf(':');
        if (secondLastColon < 0) {
            return null;
        }

        String serviceId = beforeLastColon.substring(secondLastColon + 1).trim();
        return serviceId.isEmpty() ? null : serviceId;
    }

    /**
     * Validate whether service ID is not null and conformant.
     * @param serviceId the service ID
     * @throws MetadataValidationException exception if the service ID is not conformant
     */
    public void validateServiceId(String serviceId) {
        if (StringUtils.isBlank(serviceId)) {
            throw new MetadataValidationException("The serviceId must not be null or empty. The service will not be registered in future releases.");
        }
        if (!SERVICE_ID_PATTERN.matcher(serviceId).matches()) {
            String message = String.format(
                "Invalid serviceId [%s]: must comply with RFC 952/1123 (only lowercase letters, digits, hyphens, max 63 chars). The service will not be registered in future releases.",
                serviceId
            );
            throw new MetadataValidationException(message);
        }
    }

    /**
     * Construct base URL for specific InstanceInfo.
     * Handles IPv6 addresses by formatting the hostname with brackets if needed.
     * @param instanceInfo Instance of service, for which we want to get an URL
     * @return URL to the instance
     */
    public String getUrl(InstanceInfo instanceInfo) {
        String formattedHostname = UrlUtils.formatHostnameForUrl(instanceInfo.getHostName());
        if (instanceInfo.getSecurePort() == 0 || !instanceInfo.isPortEnabled(InstanceInfo.PortType.SECURE)) {
            return "http://" + formattedHostname + ":" + instanceInfo.getPort();
        } else {
            return "https://" + formattedHostname + ":" + instanceInfo.getSecurePort();
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
