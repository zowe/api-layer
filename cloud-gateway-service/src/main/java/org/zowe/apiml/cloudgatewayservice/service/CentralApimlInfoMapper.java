package org.zowe.apiml.cloudgatewayservice.service;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
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
import static com.google.common.collect.Iterables.isEmpty;


/**
 * Build Central Registry Apiml services DTO
 */
@Component
public class CentralApimlInfoMapper {

    private static final List<String> METADATA_KEYS_WHITELIST = Arrays.asList("zos.sysname", "zos.system", "zos.sysplex", "zos.cpcName", "zos.zosName", "zos.lpar");

    public ApimlInfo buildApimlServiceInfo(String apimlId, List<ServiceInfo> gatewayServices) {
        return ApimlInfo.builder()
                .apimlId(apimlId)
                .services(gatewayServices.stream().filter(Objects::nonNull).map(this::mapServices).collect(Collectors.toList()))
                .build();
    }

    private CentralServiceInfo mapServices(ServiceInfo gws) {
        return CentralServiceInfo.builder()
                .serviceId(gws.getServiceId())
                .status(gws.getStatus())
                .apiId(extractApiId(gws).orElse(null))
                .customMetadata(extractMetadata(gws))
                .build();
    }

    private Map<String, String> extractMetadata(ServiceInfo gws) {
        if (!CollectionUtils.isEmpty(gws.getInstances())) {
            ServiceInfo.Instances firstInstance = gws.getInstances().entrySet().stream().findFirst()
                    .map(Map.Entry::getValue).orElse(null);
            if (firstInstance != null && !CollectionUtils.isEmpty(firstInstance.getCustomMetadata())) {
                return firstInstance.getCustomMetadata().entrySet().stream()
                        .filter(entry -> METADATA_KEYS_WHITELIST.contains(entry.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
        }
        return Collections.emptyMap();
    }

    private Optional<String> extractApiId(ServiceInfo gws) {
        if (gws.getApiml() != null && !isEmpty(gws.getApiml().getApiInfo())) {
            return Optional.ofNullable(emptyToNull(gws.getApiml().getApiInfo().get(0).getApiId()));
        }
        return Optional.empty();
    }
}
