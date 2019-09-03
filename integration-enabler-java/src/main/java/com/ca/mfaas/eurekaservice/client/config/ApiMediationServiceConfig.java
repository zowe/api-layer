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

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class ApiMediationServiceConfig {
    /**
     * Service configuration version. Allows to work with different versions of configuration for backward compatibility
     * of already deployed services, e.g. old versions can be converted to new versions on the fly, using converter
     * filters deployed in API ML discovery service.
     *
     * Original version 1.0 is implicit and is not stated in the YAML files.
     * Starting from version 2.0 there is a converter for the old versions, so API ML GW can expect to obtain only a
     * single actual version of metadata.
     */
    private String version;

    @Singular
    private List<String> discoveryServiceUrls;
    private String serviceId;
    private String title;
    private String description;
    private String baseUrl;
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
    private Eureka eureka;
}
