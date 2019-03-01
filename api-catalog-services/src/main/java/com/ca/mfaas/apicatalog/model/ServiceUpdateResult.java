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

import com.netflix.discovery.shared.Application;

public class ServiceUpdateResult {
    Application service;
    SERVICE_UPDATE_TYPE updateType;

    public ServiceUpdateResult() {
    }

    public void addNewService(Application service) {
        this.service = service;
        this.updateType = SERVICE_UPDATE_TYPE.NEW_SERVICE;
    }

    public void addNewInstance(Application service) {
        this.service = service;
        this.updateType = SERVICE_UPDATE_TYPE.NEW_INSTANCE;
    }

    public void removeInstance(Application service) {
        this.service = service;
        this.updateType = SERVICE_UPDATE_TYPE.REMOVED_INSTANCE;
    }

    public void updateInstance(Application service) {
        this.service = service;
        this.updateType = SERVICE_UPDATE_TYPE.UPDATED_INSTANCE;
    }

    public void renewInstance(Application service) {
        this.service = service;
        this.updateType = SERVICE_UPDATE_TYPE.RENEW_INSTANCE;
    }

    public Application getService() {
        return this.service;
    }

    public void setService(Application service) {
        this.service = service;
    }

    public com.ca.mfaas.apicatalog.model.ServiceUpdateResult.SERVICE_UPDATE_TYPE getUpdateType() {
        return this.updateType;
    }

    public void setUpdateType(com.ca.mfaas.apicatalog.model.ServiceUpdateResult.SERVICE_UPDATE_TYPE updateType) {
        this.updateType = updateType;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof com.ca.mfaas.apicatalog.model.ServiceUpdateResult)) return false;
        final com.ca.mfaas.apicatalog.model.ServiceUpdateResult other = (com.ca.mfaas.apicatalog.model.ServiceUpdateResult) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$service = this.service;
        final java.lang.Object other$service = other.service;
        if (this$service == null ? other$service != null : !this$service.equals(other$service)) return false;
        final java.lang.Object this$updateType = this.updateType;
        final java.lang.Object other$updateType = other.updateType;
        if (this$updateType == null ? other$updateType != null : !this$updateType.equals(other$updateType))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof com.ca.mfaas.apicatalog.model.ServiceUpdateResult;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $service = this.service;
        result = result * PRIME + ($service == null ? 43 : $service.hashCode());
        final java.lang.Object $updateType = this.updateType;
        result = result * PRIME + ($updateType == null ? 43 : $updateType.hashCode());
        return result;
    }

    public String toString() {
        return "ServiceUpdateResult(service=" + this.service + ", updateType=" + this.updateType + ")";
    }

    public enum SERVICE_UPDATE_TYPE {
        NEW_SERVICE,
        NEW_INSTANCE,
        REMOVED_INSTANCE,
        UPDATED_INSTANCE,
        RENEW_INSTANCE,
        CREATED_CONTAINER
    }
}
