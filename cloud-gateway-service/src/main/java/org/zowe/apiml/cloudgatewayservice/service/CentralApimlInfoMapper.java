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
        if (gatewayServices == null) {
            return ApimlInfo.builder().apimlId(apimlId).services(Collections.emptyList()).build();
        }

        return ApimlInfo.builder()
                .apimlId(apimlId)
                .services(gatewayServices.stream().filter(Objects::nonNull)
                        .map(this::mapServices).collect(Collectors.toList()))
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
        if (!isEmpty(gws.getInstances())) {
            ServiceInfo.Instances firstInstance = gws.getInstances().entrySet().stream().findFirst()
                    .map(Map.Entry::getValue).orElse(null);
            if (firstInstance != null && !isEmpty(firstInstance.getCustomMetadata())) {
                return firstInstance.getCustomMetadata().entrySet().stream()
                        .filter(entry -> METADATA_KEYS_WHITELIST.contains(entry.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
        }
        return Collections.emptyMap();
    }

    private Optional<String> extractApiId(ServiceInfo.Apiml apiml) {
        if (apiml != null && !isEmpty(apiml.getApiInfo())) {
            return Optional.ofNullable(emptyToNull(apiml.getApiInfo().get(0).getApiId()));
        }
        return Optional.empty();
    }
}
