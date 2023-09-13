package org.zowe.apiml.cloudgatewayservice.service.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ApimlInfo {

    private final String apimlId;
    private final List<CentralServiceInfo> services;
}
