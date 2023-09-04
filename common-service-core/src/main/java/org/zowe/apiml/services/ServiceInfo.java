/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.netflix.appinfo.InstanceInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.config.ApiInfo;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceInfo {
    private String serviceId;
    private InstanceInfo.InstanceStatus status;
    private Apiml apiml;
    private Map<String, Instances> instances;

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Apiml {
        private List<ApiInfoExtended> apiInfo;
        private Service service;
        private List<Authentication> authentication;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Instances {
        private InstanceInfo.InstanceStatus status;
        private String hostname;
        private String ipAddr;
        private String protocol;
        private int port;
        private String homePageUrl;
        private String healthCheckUrl;
        private String statusPageUrl;
        private Map<String, String> customMetadata;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Service {
        private String title;
        private String description;
        private String homePageUrl;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApiInfoExtended extends ApiInfo {
        private String baseUrl;
        private String basePath;
    }

}
