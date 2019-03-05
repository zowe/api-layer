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

import java.util.List;

public class APIServiceInstances {
    private List<String> instances;

    public APIServiceInstances() {
    }

    public List<String> getInstances() {
        return this.instances;
    }

    public void setInstances(List<String> instances) {
        this.instances = instances;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof com.ca.mfaas.apicatalog.services.status.model.APIServiceInstances)) return false;
        final com.ca.mfaas.apicatalog.services.status.model.APIServiceInstances other = (com.ca.mfaas.apicatalog.services.status.model.APIServiceInstances) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$instances = this.instances;
        final java.lang.Object other$instances = other.instances;
        if (this$instances == null ? other$instances != null : !this$instances.equals(other$instances)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof com.ca.mfaas.apicatalog.services.status.model.APIServiceInstances;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $instances = this.instances;
        result = result * PRIME + ($instances == null ? 43 : $instances.hashCode());
        return result;
    }

    public String toString() {
        return "APIServiceInstances(instances=" + this.instances + ")";
    }
}
