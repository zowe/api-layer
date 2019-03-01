/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.services.cached.model;

public class ApiDocCacheKey {
    private String serviceId;
    private String apiVersion;

    @java.beans.ConstructorProperties({"serviceId", "apiVersion"})
    public ApiDocCacheKey(String serviceId, String apiVersion) {
        this.serviceId = serviceId;
        this.apiVersion = apiVersion;
    }

    public String getServiceId() {
        return this.serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getApiVersion() {
        return this.apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof com.ca.mfaas.apicatalog.services.cached.model.ApiDocCacheKey)) return false;
        final com.ca.mfaas.apicatalog.services.cached.model.ApiDocCacheKey other = (com.ca.mfaas.apicatalog.services.cached.model.ApiDocCacheKey) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$serviceId = this.serviceId;
        final java.lang.Object other$serviceId = other.serviceId;
        if (this$serviceId == null ? other$serviceId != null : !this$serviceId.equals(other$serviceId)) return false;
        final java.lang.Object this$apiVersion = this.apiVersion;
        final java.lang.Object other$apiVersion = other.apiVersion;
        if (this$apiVersion == null ? other$apiVersion != null : !this$apiVersion.equals(other$apiVersion))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof com.ca.mfaas.apicatalog.services.cached.model.ApiDocCacheKey;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $serviceId = this.serviceId;
        result = result * PRIME + ($serviceId == null ? 43 : $serviceId.hashCode());
        final java.lang.Object $apiVersion = this.apiVersion;
        result = result * PRIME + ($apiVersion == null ? 43 : $apiVersion.hashCode());
        return result;
    }

    public String toString() {
        return "ApiDocCacheKey(serviceId=" + this.serviceId + ", apiVersion=" + this.apiVersion + ")";
    }
}
