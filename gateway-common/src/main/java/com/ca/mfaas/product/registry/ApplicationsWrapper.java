/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.product.registry;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.netflix.discovery.shared.Applications;

@JsonDeserialize(as = ApplicationsWrapper.class)
public class ApplicationsWrapper {

    private com.netflix.discovery.shared.Applications applications;

    public ApplicationsWrapper() {
    }

    public Applications getApplications() {
        return this.applications;
    }

    public void setApplications(Applications applications) {
        this.applications = applications;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ApplicationsWrapper)) return false;
        final ApplicationsWrapper other = (ApplicationsWrapper) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$applications = this.getApplications();
        final Object other$applications = other.getApplications();
        if (this$applications == null ? other$applications != null : !this$applications.equals(other$applications))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ApplicationsWrapper;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $applications = this.getApplications();
        result = result * PRIME + ($applications == null ? 43 : $applications.hashCode());
        return result;
    }

    public String toString() {
        return "ApplicationsWrapper(applications=" + this.getApplications() + ")";
    }
}
