package org.zowe.apiml.cloudgatewayservice.service.model;

import com.netflix.appinfo.InstanceInfo;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class CentralServiceInfo {

    private final InstanceInfo.InstanceStatus status;
    private final Map<String,String> customMetadata;
    private final String apiId;
    private final String serviceId;

}
