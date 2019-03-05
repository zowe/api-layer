/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.model;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

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

    @ApiModelProperty(notes = "The service home page of the API service")
    private String homePageUrl;

    @ApiModelProperty(notes = "The API documentation for this service")
    private String apiDoc;

    public APIService(String serviceId, String title, String description, boolean secured, String homePageUrl) {
        this.serviceId = serviceId;
        this.title = title;
        this.description = description;
        this.status = "UP";
        this.secured = secured;
        this.homePageUrl = homePageUrl;
        this.apiDoc = null;
    }

    public APIService(String serviceId, String title, String description, boolean secured, String homePageUrl, String apiDoc) {
        this.serviceId = serviceId;
        this.title = title;
        this.description = description;
        this.status = "UP";
        this.secured = secured;
        this.homePageUrl = homePageUrl;
        this.apiDoc = apiDoc;
    }

    public String getServiceId() {
        return this.serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isSecured() {
        return this.secured;
    }

    public void setSecured(boolean secured) {
        this.secured = secured;
    }

    public String getHomePageUrl() {
        return this.homePageUrl;
    }

    public void setHomePageUrl(String homePageUrl) {
        this.homePageUrl = homePageUrl;
    }

    public String getApiDoc() {
        return this.apiDoc;
    }

    public void setApiDoc(String apiDoc) {
        this.apiDoc = apiDoc;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof com.ca.mfaas.apicatalog.model.APIService)) return false;
        final com.ca.mfaas.apicatalog.model.APIService other = (com.ca.mfaas.apicatalog.model.APIService) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$serviceId = this.serviceId;
        final java.lang.Object other$serviceId = other.serviceId;
        if (this$serviceId == null ? other$serviceId != null : !this$serviceId.equals(other$serviceId)) return false;
        final java.lang.Object this$title = this.title;
        final java.lang.Object other$title = other.title;
        if (this$title == null ? other$title != null : !this$title.equals(other$title)) return false;
        final java.lang.Object this$description = this.description;
        final java.lang.Object other$description = other.description;
        if (this$description == null ? other$description != null : !this$description.equals(other$description))
            return false;
        final java.lang.Object this$status = this.status;
        final java.lang.Object other$status = other.status;
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        if (this.secured != other.secured) return false;
        final java.lang.Object this$homePageUrl = this.homePageUrl;
        final java.lang.Object other$homePageUrl = other.homePageUrl;
        if (this$homePageUrl == null ? other$homePageUrl != null : !this$homePageUrl.equals(other$homePageUrl))
            return false;
        final java.lang.Object this$apiDoc = this.apiDoc;
        final java.lang.Object other$apiDoc = other.apiDoc;
        if (this$apiDoc == null ? other$apiDoc != null : !this$apiDoc.equals(other$apiDoc)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof com.ca.mfaas.apicatalog.model.APIService;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $serviceId = this.serviceId;
        result = result * PRIME + ($serviceId == null ? 43 : $serviceId.hashCode());
        final java.lang.Object $title = this.title;
        result = result * PRIME + ($title == null ? 43 : $title.hashCode());
        final java.lang.Object $description = this.description;
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final java.lang.Object $status = this.status;
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        result = result * PRIME + (this.secured ? 79 : 97);
        final java.lang.Object $homePageUrl = this.homePageUrl;
        result = result * PRIME + ($homePageUrl == null ? 43 : $homePageUrl.hashCode());
        final java.lang.Object $apiDoc = this.apiDoc;
        result = result * PRIME + ($apiDoc == null ? 43 : $apiDoc.hashCode());
        return result;
    }

    public String toString() {
        return "APIService(serviceId=" + this.serviceId + ", title=" + this.title + ", description=" + this.description + ", status=" + this.status + ", secured=" + this.secured + ", homePageUrl=" + this.homePageUrl + ", apiDoc=" + this.apiDoc + ")";
    }
}
