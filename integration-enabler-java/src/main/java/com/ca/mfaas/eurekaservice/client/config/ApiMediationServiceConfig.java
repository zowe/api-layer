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

import lombok.*;

import java.util.List;

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
    private String homePageRelativeUrl;
    private String statusPageRelativeUrl;
    private String healthCheckRelativeUrl;
    private String contextPath;
    private String defaultZone;
    private Boolean securePortEnabled;
    @Singular
    private List<Route> routes;
    private ApiInfo apiInfo;
    private CatalogUiTile catalogUiTile;
    private Ssl ssl;
    private Eureka eureka;
}
