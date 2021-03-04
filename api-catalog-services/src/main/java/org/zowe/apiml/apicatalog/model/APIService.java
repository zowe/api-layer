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

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(of = {"serviceId"})
public class APIService implements Serializable {

    private static final long serialVersionUID = 5119572678327579985L;

    @ApiModelProperty(notes = "The service id")
    private String serviceId;

    @ApiModelProperty(notes = "The API service name")
    private String title;

    @ApiModelProperty(notes = "The description of the API service")
    private String description;

    @ApiModelProperty(notes = "The status of the API service")
    private String status;

    @ApiModelProperty(notes = "The security status of the API service")
    private boolean secured;

    @ApiModelProperty(notes = "The instance home URL")
    private String baseUrl;

    @ApiModelProperty(notes = "The service home page of the API service")
    private String homePageUrl;

    @ApiModelProperty(notes = "The service API base path of the API service")
    private String basePath;

    @ApiModelProperty(notes = "The API documentation for this service")
    private String apiDoc;

    @ApiModelProperty(notes = "The default API version for this service")
    private String defaultApiVersion = "v1";

    @ApiModelProperty(notes = "The available API versions for this service")
    private List<String> apiVersions;

    @ApiModelProperty(notes = "The SSO support for this instance")
    private boolean sso;

    @ApiModelProperty(notes = "The SSO support for all instances")
    private boolean ssoAllInstances;

    @ApiModelProperty(notes = "The API ID for this service")
    private Map<String, String> apiId;

    private APIService(String serviceId) {
        this.serviceId = serviceId;
        this.status = "UP";
    }
    public static class Builder {
        private APIService apiService;
        
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

        public Builder apiId(Map<String, String> apiId) {
            apiService.apiId = apiId;
            return this;
        }

        public APIService build() {
            return apiService;
        }
    }

}
