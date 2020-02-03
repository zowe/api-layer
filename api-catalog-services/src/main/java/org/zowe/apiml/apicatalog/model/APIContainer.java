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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class APIContainer implements Serializable {

    private static final long serialVersionUID = 1905122041950251207L;

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
}
