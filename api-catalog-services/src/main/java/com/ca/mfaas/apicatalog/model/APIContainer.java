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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@JsonIgnoreProperties(ignoreUnknown = true)
public class APIContainer implements Serializable {

    private static final long serialVersionUID = 1905122041950251207L;
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(APIContainer.class);

    @ApiModelProperty(notes = "The version of the API container")
    private String version;

    @ApiModelProperty(notes = "The API Container Id")
    private String id;

    @ApiModelProperty(notes = "The API Container title")
    private String title;

    @ApiModelProperty(notes = "The Status of the container")
    private String status;

    @ApiModelProperty(notes = "The description of the API")
    private String description;

    @ApiModelProperty(notes = "A collection of services which are registered with this API")
    private Set<APIService> services;

    private Integer totalServices;

    private Integer activeServices;

    // used to determine time in cache
    private Calendar lastUpdatedTimestamp;

    // used to determine if container is new
    private Calendar createdTimestamp;

    public APIContainer() {
        this.lastUpdatedTimestamp = Calendar.getInstance();
        this.createdTimestamp = this.lastUpdatedTimestamp;
        this.version = "1.0.0";
    }

    public APIContainer(String id, String title, String description, Set<APIService> services) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.services = services;
        this.lastUpdatedTimestamp = Calendar.getInstance();
        this.createdTimestamp = this.lastUpdatedTimestamp;
        this.version = "1.0.0";
    }

    public void addService(APIService apiService) {
        if (services == null) {
            services = new HashSet<>();
        }
        boolean match = services.stream().anyMatch(service -> service.getServiceId().equalsIgnoreCase(apiService.getServiceId()));
        if (!match) {
            services.add(apiService);
            updateLastUpdatedTimestamp();
        }
    }

    /**
     * Update the last updated timestamp to now
     */
    public void updateLastUpdatedTimestamp() {
        this.lastUpdatedTimestamp = Calendar.getInstance();
    }

    /**
     * Has this container been updated within the timeframe specified by the threshold interval
     *
     * @param thresholdInMillis if the update time is after the threshold time then this is a recent update
     * @return true if updated recently
     */
    public boolean isRecentUpdated(int thresholdInMillis) {
        boolean isRecent;
        Calendar threshold = Calendar.getInstance();
        threshold.add(Calendar.MILLISECOND, -thresholdInMillis);
        isRecent = threshold.before(this.lastUpdatedTimestamp);
        if (isRecent) {
            log.debug("\nContainer: " + this.getId() + " has been updated within the given threshold of " + thresholdInMillis / 1000
                + " seconds.\nThreshold: " + threshold.getTime() + "\nThis: " + this.lastUpdatedTimestamp.getTime()
                + "\ntime difference(millis): " +
                TimeUnit.MILLISECONDS.toSeconds(this.lastUpdatedTimestamp.getTimeInMillis() - threshold.getTimeInMillis())
                + " recently updated\n");
        }
        return isRecent;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<APIService> getServices() {
        return this.services;
    }

    public void setServices(Set<APIService> services) {
        this.services = services;
    }

    public Integer getTotalServices() {
        return this.totalServices;
    }

    public void setTotalServices(Integer totalServices) {
        this.totalServices = totalServices;
    }

    public Integer getActiveServices() {
        return this.activeServices;
    }

    public void setActiveServices(Integer activeServices) {
        this.activeServices = activeServices;
    }

    public Calendar getLastUpdatedTimestamp() {
        return this.lastUpdatedTimestamp;
    }

    public void setLastUpdatedTimestamp(Calendar lastUpdatedTimestamp) {
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
    }

    public Calendar getCreatedTimestamp() {
        return this.createdTimestamp;
    }

    public void setCreatedTimestamp(Calendar createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof com.ca.mfaas.apicatalog.model.APIContainer)) return false;
        final com.ca.mfaas.apicatalog.model.APIContainer other = (com.ca.mfaas.apicatalog.model.APIContainer) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$version = this.version;
        final java.lang.Object other$version = other.version;
        if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
        final java.lang.Object this$id = this.id;
        final java.lang.Object other$id = other.id;
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$title = this.title;
        final java.lang.Object other$title = other.title;
        if (this$title == null ? other$title != null : !this$title.equals(other$title)) return false;
        final java.lang.Object this$status = this.status;
        final java.lang.Object other$status = other.status;
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$description = this.description;
        final java.lang.Object other$description = other.description;
        if (this$description == null ? other$description != null : !this$description.equals(other$description))
            return false;
        final java.lang.Object this$services = this.services;
        final java.lang.Object other$services = other.services;
        if (this$services == null ? other$services != null : !this$services.equals(other$services)) return false;
        final java.lang.Object this$totalServices = this.totalServices;
        final java.lang.Object other$totalServices = other.totalServices;
        if (this$totalServices == null ? other$totalServices != null : !this$totalServices.equals(other$totalServices))
            return false;
        final java.lang.Object this$activeServices = this.activeServices;
        final java.lang.Object other$activeServices = other.activeServices;
        if (this$activeServices == null ? other$activeServices != null : !this$activeServices.equals(other$activeServices))
            return false;
        final java.lang.Object this$lastUpdatedTimestamp = this.lastUpdatedTimestamp;
        final java.lang.Object other$lastUpdatedTimestamp = other.lastUpdatedTimestamp;
        if (this$lastUpdatedTimestamp == null ? other$lastUpdatedTimestamp != null : !this$lastUpdatedTimestamp.equals(other$lastUpdatedTimestamp))
            return false;
        final java.lang.Object this$createdTimestamp = this.createdTimestamp;
        final java.lang.Object other$createdTimestamp = other.createdTimestamp;
        if (this$createdTimestamp == null ? other$createdTimestamp != null : !this$createdTimestamp.equals(other$createdTimestamp))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof com.ca.mfaas.apicatalog.model.APIContainer;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $version = this.version;
        result = result * PRIME + ($version == null ? 43 : $version.hashCode());
        final java.lang.Object $id = this.id;
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $title = this.title;
        result = result * PRIME + ($title == null ? 43 : $title.hashCode());
        final java.lang.Object $status = this.status;
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $description = this.description;
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final java.lang.Object $services = this.services;
        result = result * PRIME + ($services == null ? 43 : $services.hashCode());
        final java.lang.Object $totalServices = this.totalServices;
        result = result * PRIME + ($totalServices == null ? 43 : $totalServices.hashCode());
        final java.lang.Object $activeServices = this.activeServices;
        result = result * PRIME + ($activeServices == null ? 43 : $activeServices.hashCode());
        final java.lang.Object $lastUpdatedTimestamp = this.lastUpdatedTimestamp;
        result = result * PRIME + ($lastUpdatedTimestamp == null ? 43 : $lastUpdatedTimestamp.hashCode());
        final java.lang.Object $createdTimestamp = this.createdTimestamp;
        result = result * PRIME + ($createdTimestamp == null ? 43 : $createdTimestamp.hashCode());
        return result;
    }

    public String toString() {
        return "APIContainer(version=" + this.version + ", id=" + this.id + ", title=" + this.title + ", status=" + this.status + ", description=" + this.description + ", services=" + this.services + ", totalServices=" + this.totalServices + ", activeServices=" + this.activeServices + ", lastUpdatedTimestamp=" + this.lastUpdatedTimestamp + ", createdTimestamp=" + this.createdTimestamp + ")";
    }
}
