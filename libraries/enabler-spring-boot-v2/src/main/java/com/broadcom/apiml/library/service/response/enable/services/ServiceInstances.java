/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.response.enable.services;

import com.netflix.appinfo.InstanceInfo;
import org.springframework.cloud.client.ServiceInstance;

import java.util.List;

public class ServiceInstances {
    private List<InstanceInfo> instanceInfos;
    private List<ServiceInstance> serviceInstances;

    public ServiceInstances() {
    }

    public List<InstanceInfo> getInstanceInfos() {
        return this.instanceInfos;
    }

    public void setInstanceInfos(List<InstanceInfo> instanceInfos) {
        this.instanceInfos = instanceInfos;
    }

    public List<ServiceInstance> getServiceInstances() {
        return this.serviceInstances;
    }

    public void setServiceInstances(List<ServiceInstance> serviceInstances) {
        this.serviceInstances = serviceInstances;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ServiceInstances)) return false;
        final ServiceInstances other = (ServiceInstances) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$instanceInfos = this.getInstanceInfos();
        final Object other$instanceInfos = other.getInstanceInfos();
        if (this$instanceInfos == null ? other$instanceInfos != null : !this$instanceInfos.equals(other$instanceInfos))
            return false;
        final Object this$serviceInstances = this.getServiceInstances();
        final Object other$serviceInstances = other.getServiceInstances();
        if (this$serviceInstances == null ? other$serviceInstances != null : !this$serviceInstances.equals(other$serviceInstances))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ServiceInstances;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $instanceInfos = this.getInstanceInfos();
        result = result * PRIME + ($instanceInfos == null ? 43 : $instanceInfos.hashCode());
        final Object $serviceInstances = this.getServiceInstances();
        result = result * PRIME + ($serviceInstances == null ? 43 : $serviceInstances.hashCode());
        return result;
    }

    public String toString() {
        return "ServiceInstances(instanceInfos=" + this.getInstanceInfos() + ", serviceInstances=" + this.getServiceInstances() + ")";
    }
}
