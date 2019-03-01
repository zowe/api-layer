/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.enable.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(as = ApplicationWrapper.class)
public class ApplicationWrapper {

    private com.netflix.discovery.shared.Application application;

    public ApplicationWrapper() {
    }

    public Application getApplication() {
        return this.application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof com.ca.mfaas.enable.model.ApplicationWrapper)) return false;
        final com.ca.mfaas.enable.model.ApplicationWrapper other = (com.ca.mfaas.enable.model.ApplicationWrapper) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$application = this.application;
        final java.lang.Object other$application = other.application;
        if (this$application == null ? other$application != null : !this$application.equals(other$application))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof com.ca.mfaas.enable.model.ApplicationWrapper;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $application = this.application;
        result = result * PRIME + ($application == null ? 43 : $application.hashCode());
        return result;
    }

    public String toString() {
        return "ApplicationWrapper(application=" + this.application + ")";
    }
}
