/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.services.status.model;

public class APIServiceInfo {
    private String instanceId;

    public APIServiceInfo() {
    }

    public String getInstanceId() {
        return this.instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof com.ca.mfaas.apicatalog.services.status.model.APIServiceInfo)) return false;
        final com.ca.mfaas.apicatalog.services.status.model.APIServiceInfo other = (com.ca.mfaas.apicatalog.services.status.model.APIServiceInfo) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$instanceId = this.instanceId;
        final java.lang.Object other$instanceId = other.instanceId;
        if (this$instanceId == null ? other$instanceId != null : !this$instanceId.equals(other$instanceId))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof com.ca.mfaas.apicatalog.services.status.model.APIServiceInfo;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $instanceId = this.instanceId;
        result = result * PRIME + ($instanceId == null ? 43 : $instanceId.hashCode());
        return result;
    }

    public String toString() {
        return "APIServiceInfo(instanceId=" + this.instanceId + ")";
    }
}
