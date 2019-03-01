/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.services.status.event.model;

import com.ca.mfaas.apicatalog.model.APIService;

import java.util.Set;

public class ContainerStatusChangeEvent implements StatusChangeEvent {
    private final String containerId;
    private final String title;
    private final String status;
    private final int totalServices;
    private final int activeServices;
    private final STATUS_EVENT_TYPE statusEventType;
    private final String timeStamp;
    private Set<APIService> services;

    public ContainerStatusChangeEvent(String containerId, String title, String status,
                                      int totalServices, int activeServices,
                                      Set<APIService> services,
                                      STATUS_EVENT_TYPE statusEventType) {
        this.containerId = containerId;
        this.title = title;
        this.status = status;
        this.services = services;
        this.totalServices = totalServices;
        this.activeServices = activeServices;
        this.statusEventType = statusEventType;
        timeStamp = setTimeStamp();
    }

    public String getContainerId() {
        return this.containerId;
    }

    public String getTitle() {
        return this.title;
    }

    public String getStatus() {
        return this.status;
    }

    public int getTotalServices() {
        return this.totalServices;
    }

    public int getActiveServices() {
        return this.activeServices;
    }

    public STATUS_EVENT_TYPE getStatusEventType() {
        return this.statusEventType;
    }

    public String getTimeStamp() {
        return this.timeStamp;
    }

    public Set<APIService> getServices() {
        return this.services;
    }

    public void setServices(Set<APIService> services) {
        this.services = services;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof com.ca.mfaas.apicatalog.services.status.event.model.ContainerStatusChangeEvent))
            return false;
        final com.ca.mfaas.apicatalog.services.status.event.model.ContainerStatusChangeEvent other = (com.ca.mfaas.apicatalog.services.status.event.model.ContainerStatusChangeEvent) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$containerId = this.containerId;
        final java.lang.Object other$containerId = other.containerId;
        if (this$containerId == null ? other$containerId != null : !this$containerId.equals(other$containerId))
            return false;
        final java.lang.Object this$title = this.title;
        final java.lang.Object other$title = other.title;
        if (this$title == null ? other$title != null : !this$title.equals(other$title)) return false;
        final java.lang.Object this$status = this.status;
        final java.lang.Object other$status = other.status;
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        if (this.totalServices != other.totalServices) return false;
        if (this.activeServices != other.activeServices) return false;
        final java.lang.Object this$statusEventType = this.statusEventType;
        final java.lang.Object other$statusEventType = other.statusEventType;
        if (this$statusEventType == null ? other$statusEventType != null : !this$statusEventType.equals(other$statusEventType))
            return false;
        final java.lang.Object this$timeStamp = this.timeStamp;
        final java.lang.Object other$timeStamp = other.timeStamp;
        if (this$timeStamp == null ? other$timeStamp != null : !this$timeStamp.equals(other$timeStamp)) return false;
        final java.lang.Object this$services = this.services;
        final java.lang.Object other$services = other.services;
        if (this$services == null ? other$services != null : !this$services.equals(other$services)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof com.ca.mfaas.apicatalog.services.status.event.model.ContainerStatusChangeEvent;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $containerId = this.containerId;
        result = result * PRIME + ($containerId == null ? 43 : $containerId.hashCode());
        final java.lang.Object $title = this.title;
        result = result * PRIME + ($title == null ? 43 : $title.hashCode());
        final java.lang.Object $status = this.status;
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        result = result * PRIME + this.totalServices;
        result = result * PRIME + this.activeServices;
        final java.lang.Object $statusEventType = this.statusEventType;
        result = result * PRIME + ($statusEventType == null ? 43 : $statusEventType.hashCode());
        final java.lang.Object $timeStamp = this.timeStamp;
        result = result * PRIME + ($timeStamp == null ? 43 : $timeStamp.hashCode());
        final java.lang.Object $services = this.services;
        result = result * PRIME + ($services == null ? 43 : $services.hashCode());
        return result;
    }

    public String toString() {
        return "ContainerStatusChangeEvent(containerId=" + this.containerId + ", title=" + this.title + ", status=" + this.status + ", totalServices=" + this.totalServices + ", activeServices=" + this.activeServices + ", statusEventType=" + this.statusEventType + ", timeStamp=" + this.timeStamp + ", services=" + this.services + ")";
    }
}
