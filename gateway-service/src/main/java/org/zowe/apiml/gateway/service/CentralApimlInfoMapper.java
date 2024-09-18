/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.service;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.gateway.service.model.ApimlInfo;
import org.zowe.apiml.gateway.service.model.CentralServiceInfo;
import org.zowe.apiml.services.ServiceInfo;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.util.CollectionUtils.isEmpty;


/**
 * Build Central Registry Apiml services DTO
 */
@Component
public class CentralApimlInfoMapper {

    @Value("${apiml.gateway.registry.metadata-key-allow-list:}")
    Set<String> metadataKeysAllowList = new HashSet<>();

    public ApimlInfo buildApimlServiceInfo(@NonNull String apimlId, List<ServiceInfo> gatewayServices) {
        List<CentralServiceInfo> services = Optional.ofNullable(gatewayServices).orElse(Collections.emptyList()).stream()
            .filter(Objects::nonNull)
            .map(this::mapServices)
            .toList();

        return ApimlInfo.builder()
            .apimlId(apimlId)
            .services(services)
            .build();
    }

    private CentralServiceInfo mapServices(ServiceInfo gws) {
        return CentralServiceInfo.builder()
            .serviceId(gws.getServiceId())
            .status(gws.getStatus())
            .apiId(extractApiId(gws.getApiml()))
            .customMetadata(extractMetadata(gws))
            .build();
    }

    private Map<String, String> extractMetadata(ServiceInfo gws) {
        return Optional.ofNullable(gws.getInstances()).orElseGet(Collections::emptyMap)
            .entrySet().stream()
            .findFirst()
            .map(Map.Entry::getValue)
            .filter(i -> !isEmpty(i.getCustomMetadata()))
            .map(ServiceInfo.Instances::getCustomMetadata)
            .orElse(Collections.emptyMap())
            .entrySet().stream()
            .filter(entry -> metadataKeysAllowList.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Set<String> extractApiId(ServiceInfo.Apiml apiml) {
        return Optional.ofNullable(apiml)
            .map(ServiceInfo.Apiml::getApiInfo)
            .orElse(Collections.emptyList()).stream()
            .map(ApiInfo::getApiId)
            .collect(Collectors.toSet());
    }
}
