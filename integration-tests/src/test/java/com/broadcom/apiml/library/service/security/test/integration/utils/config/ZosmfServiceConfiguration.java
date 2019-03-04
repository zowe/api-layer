/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.security.test.integration.utils.config;

public class ZosmfServiceConfiguration {
    private String scheme;
    private String host;
    private int port;

    @java.beans.ConstructorProperties({"scheme", "host", "port"})
    public ZosmfServiceConfiguration(String scheme, String host, int port) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
    }

    public ZosmfServiceConfiguration() {
    }

    public String getScheme() {
        return this.scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getHost() {
        return this.host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ZosmfServiceConfiguration)) return false;
        final ZosmfServiceConfiguration other = (ZosmfServiceConfiguration) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$scheme = this.getScheme();
        final Object other$scheme = other.getScheme();
        if (this$scheme == null ? other$scheme != null : !this$scheme.equals(other$scheme)) return false;
        final Object this$host = this.getHost();
        final Object other$host = other.getHost();
        if (this$host == null ? other$host != null : !this$host.equals(other$host)) return false;
        if (this.getPort() != other.getPort()) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ZosmfServiceConfiguration;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $scheme = this.getScheme();
        result = result * PRIME + ($scheme == null ? 43 : $scheme.hashCode());
        final Object $host = this.getHost();
        result = result * PRIME + ($host == null ? 43 : $host.hashCode());
        result = result * PRIME + this.getPort();
        return result;
    }

    public String toString() {
        return "ZosmfServiceConfiguration(scheme=" + this.getScheme() + ", host=" + this.getHost() + ", port=" + this.getPort() + ")";
    }
}
