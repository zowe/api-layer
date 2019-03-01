/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.sample.enable.v1.controller;

/**
 * Sample class for returning data
 */
public class Sample {
    private String name;
    private String details;
    private int index;

    @java.beans.ConstructorProperties({"name", "details", "index"})
    public Sample(String name, String details, int index) {
        this.name = name;
        this.details = details;
        this.index = index;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDetails() {
        return this.details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof com.ca.mfaas.sample.enable.v1.controller.Sample)) return false;
        final com.ca.mfaas.sample.enable.v1.controller.Sample other = (com.ca.mfaas.sample.enable.v1.controller.Sample) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$name = this.name;
        final java.lang.Object other$name = other.name;
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final java.lang.Object this$details = this.details;
        final java.lang.Object other$details = other.details;
        if (this$details == null ? other$details != null : !this$details.equals(other$details)) return false;
        if (this.index != other.index) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof com.ca.mfaas.sample.enable.v1.controller.Sample;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $name = this.name;
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final java.lang.Object $details = this.details;
        result = result * PRIME + ($details == null ? 43 : $details.hashCode());
        result = result * PRIME + this.index;
        return result;
    }

    public String toString() {
        return "Sample(name=" + this.name + ", details=" + this.details + ", index=" + this.index + ")";
    }
}
