/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.zowe.apiml.config.ApiInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(of = {"serviceId"})
public class APIService implements Serializable {

    private static final long serialVersionUID = 5119572678327579985L;

    @Schema(description = "The service id")
    private String serviceId;

    @Schema(description = "The API service name")
    private String title;

    @Schema(description = "The description of the API service")
    private String description;

    @Schema(description = "The status of the API service")
    private String status;

    @Schema(description = "The security status of the API service")
    private boolean secured;

    @Schema(description = "The instance home URL")
    private String baseUrl;

    @Schema(description = "The service home page of the API service")
    private String homePageUrl;

    @Schema(description = "The service API base path of the API service")
    private String basePath;

    @Schema(description = "The API documentation for this service")
    private String apiDoc;

    @Schema(description = "The default API version for this service")
    private String defaultApiVersion = "v1";

    @Schema(description = "The error message occurred when trying to retrieve the API doc")
    private String apiDocErrorMessage;

    @Schema(description = "The available API versions for this service")
    private List<String> apiVersions;

    @Schema(description = "The SSO support for this instance")
    private boolean sso;

    @Schema(description = "The SSO support for all instances")
    private boolean ssoAllInstances;

    @Schema(description = "The API information for each API ID for this service")
    private Map<String, ApiInfo> apis = new HashMap<>(); // NOSONAR

    private List<String> instances = new ArrayList<>();

    private APIService(String serviceId) {
        this.serviceId = serviceId;
        this.status = "UP";
    }

    public static class Builder {
        private final APIService apiService;

        public Builder(String serviceId) {
            apiService = new APIService(serviceId);
            apiService.status = "UP";
        }

        public Builder title(String title) {
            apiService.title = title;
            return this;
        }

        public Builder description(String description) {
            apiService.description = description;
            return this;
        }

        public Builder status(String status) {
            apiService.status = status;
            return this;
        }

        public Builder secured(boolean secured) {
            apiService.secured = secured;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            apiService.baseUrl = baseUrl;
            return this;
        }

        public Builder homePageUrl(String homePageUrl) {
            apiService.homePageUrl = homePageUrl;
            return this;
        }

        public Builder basePath(String basePath) {
            apiService.basePath = basePath;
            return this;
        }

        public Builder apiDoc(String apiDoc) {
            apiService.apiDoc = apiDoc;
            return this;
        }

        public Builder sso(boolean sso) {
            apiService.sso = sso;
            return this;
        }

        public Builder instanceId(String id) {
            apiService.instances.add(id);
            return this;
        }

        public Builder apis(Map<String, ApiInfo> apis) {
            apiService.apis = apis;
            return this;
        }

        public APIService build() {
            return apiService;
        }
    }

}
