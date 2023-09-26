/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.service;

import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.zowe.apiml.cloudgatewayservice.service.model.ApimlInfo;
import org.zowe.apiml.cloudgatewayservice.service.model.CentralServiceInfo;
import org.zowe.apiml.services.ServiceInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.emptyToNull;
import static org.springframework.util.CollectionUtils.isEmpty;


/**
 * Build Central Registry Apiml services DTO
 */
@Component
public class CentralApimlInfoMapper {
    private static final List<String> METADATA_KEYS_WHITELIST = Arrays.asList("zos.sysname", "zos.system", "zos.sysplex", "zos.cpcName", "zos.zosName", "zos.lpar");

    public ApimlInfo buildApimlServiceInfo(@NonNull String apimlId, List<ServiceInfo> gatewayServices) {
        List<CentralServiceInfo> services = Optional.ofNullable(gatewayServices).else(Collection.emptyList()).stream()
            .filter(Objects::nonNull)
            .map(this::mapServices)
            .collect(Collectors.toList()));

        return ApimlInfo.builder()
                .apimlId(apimlId)
                .services(services)
                .build();
    }

    private CentralServiceInfo mapServices(ServiceInfo gws) {
        return CentralServiceInfo.builder()
                .serviceId(gws.getServiceId())
                .status(gws.getStatus())
                .apiId(extractApiId(gws.getApiml()).orElse(null))
                .customMetadata(extractMetadata(gws))
                .build();
    }

    private Map<String, String> extractMetadata(ServiceInfo gws) {
        return ServiceInfo.Instances firstInstance = gws.getInstances().entrySet().stream().findFirst()
                    .map(Map.Entry::getValue)
                    .filter(i -> !isEmpty(i.getCustomMetadata()))
                    .map(ServiceInfo.Instances::getCustomMetadata)
                    .map(Map::entrySet)
                    .map(Collection::stream)
                    .filter(entry -> METADATA_KEYS_WHITELIST.contains(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                    .orElse(Collections.emptyMap());
    }

    private Optional<String> extractApiId(ServiceInfo.Apiml apiml) {
        return Optional.ofNullable(apiml).map(ServiceInfo.Apiml::getApiInfo).map(x -> x.get(0)).map(ApiInfo::getApiId);
    }
}
