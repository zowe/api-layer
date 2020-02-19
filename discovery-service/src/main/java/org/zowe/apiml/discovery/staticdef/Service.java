/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.discovery.staticdef;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.security.common.auth.Authentication;

import java.util.List;
import java.util.Map;

/**
 * Represents one services with multiple instances.
 *
 * Each instance can have different base URL (http(s)://hostname:port/contextPath/).
 * The other URLs are relative to it.
 */
 @Data class Service {
    private String serviceId;
    private String title;
    private String description;
    private String catalogUiTileId;
    private List<String> instanceBaseUrls;
    private String homePageRelativeUrl;
    private String statusPageRelativeUrl;
    private String healthCheckRelativeUrl;
    @JsonAlias({"routedServices"})
    private List<Route> routes;
    private List<ApiInfo> apiInfo;
    private Authentication authentication;
    private Map<String, Object> customMetadata;
}
