/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.broadcom.apiml.test.integration.eurekaservice.client.config;

public class Eureka {
    private String port;
    private String hostname;
    private String ipAddress;

    @java.beans.ConstructorProperties({"port", "hostname", "ipAddress"})
    public Eureka(String port, String hostname, String ipAddress) {
        this.port = port;
        this.hostname = hostname;
        this.ipAddress = ipAddress;
    }

    public Eureka() {
    }

    public String getPort() {
        return this.port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getHostname() {
        return this.hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Eureka)) return false;
        final Eureka other = (Eureka) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$port = this.getPort();
        final Object other$port = other.getPort();
        if (this$port == null ? other$port != null : !this$port.equals(other$port)) return false;
        final Object this$hostname = this.getHostname();
        final Object other$hostname = other.getHostname();
        if (this$hostname == null ? other$hostname != null : !this$hostname.equals(other$hostname)) return false;
        final Object this$ipAddress = this.getIpAddress();
        final Object other$ipAddress = other.getIpAddress();
        if (this$ipAddress == null ? other$ipAddress != null : !this$ipAddress.equals(other$ipAddress)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Eureka;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $port = this.getPort();
        result = result * PRIME + ($port == null ? 43 : $port.hashCode());
        final Object $hostname = this.getHostname();
        result = result * PRIME + ($hostname == null ? 43 : $hostname.hashCode());
        final Object $ipAddress = this.getIpAddress();
        result = result * PRIME + ($ipAddress == null ? 43 : $ipAddress.hashCode());
        return result;
    }

    public String toString() {
        return "Eureka(port=" + this.getPort() + ", hostname=" + this.getHostname() + ", ipAddress=" + this.getIpAddress() + ")";
    }
}
