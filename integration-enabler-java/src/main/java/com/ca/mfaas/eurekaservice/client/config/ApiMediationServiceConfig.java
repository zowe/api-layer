/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.eurekaservice.client.config;

import com.ca.mfaas.config.ApiInfo;
import lombok.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class ApiMediationServiceConfig {
    @Singular
    private List<String> discoveryServiceUrls;
    private String serviceId;
    private String title;
    private String description;
    private String baseUrl;
    private String serviceIpAddress;
    private String homePageRelativeUrl;
    private String statusPageRelativeUrl;
    private String healthCheckRelativeUrl;
    private String contextPath;
    private String defaultZone;
    private Boolean securePortEnabled;
    @Singular
    private List<Route> routes;
    private List<ApiInfo> apiInfo;
    private Catalog catalog;
    private Ssl ssl;

    // TODO: Find elegant way to generate the map with keys in property format, e.g. "this.is.a.property"
    // TODO: Use lombok if possible or use introspection
    public Map<String, Object> asMap() {
        Map<String, Object> aMap = new HashMap<>();
        aMap.put("serviceId", serviceId);
        aMap.put("title",title );
        aMap.put("description", description);
        aMap.put("baseUrl", baseUrl);
        aMap.put("homePageRelativeUrl", homePageRelativeUrl);
        aMap.put("statusPageRelativeUrl", statusPageRelativeUrl);
        aMap.put("healthCheckRelativeUrl", healthCheckRelativeUrl);
        aMap.put("contextPath", contextPath);
        aMap.put("defaultZone", defaultZone);
        aMap.put("securePortEnabled", securePortEnabled);
        // TODO: Add the rest of the properties: Ssl....
        return aMap;
    }
}
