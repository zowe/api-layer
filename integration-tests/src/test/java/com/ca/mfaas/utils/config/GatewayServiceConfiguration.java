/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.utils.config;

public class GatewayServiceConfiguration {
    private String scheme;
    private String host;
    private int port;
    private String user;
    private String password;
    private int instances;

    @java.beans.ConstructorProperties({"scheme", "host", "port", "user", "password", "instances"})
    public GatewayServiceConfiguration(String scheme, String host, int port, String user, String password, int instances) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.instances = instances;
    }

    public GatewayServiceConfiguration() {
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

    public String getUser() {
        return this.user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getInstances() {
        return this.instances;
    }

    public void setInstances(int instances) {
        this.instances = instances;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof GatewayServiceConfiguration)) return false;
        final GatewayServiceConfiguration other = (GatewayServiceConfiguration) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$scheme = this.getScheme();
        final Object other$scheme = other.getScheme();
        if (this$scheme == null ? other$scheme != null : !this$scheme.equals(other$scheme)) return false;
        final Object this$host = this.getHost();
        final Object other$host = other.getHost();
        if (this$host == null ? other$host != null : !this$host.equals(other$host)) return false;
        if (this.getPort() != other.getPort()) return false;
        final Object this$user = this.getUser();
        final Object other$user = other.getUser();
        if (this$user == null ? other$user != null : !this$user.equals(other$user)) return false;
        final Object this$password = this.getPassword();
        final Object other$password = other.getPassword();
        if (this$password == null ? other$password != null : !this$password.equals(other$password)) return false;
        if (this.getInstances() != other.getInstances()) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof GatewayServiceConfiguration;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $scheme = this.getScheme();
        result = result * PRIME + ($scheme == null ? 43 : $scheme.hashCode());
        final Object $host = this.getHost();
        result = result * PRIME + ($host == null ? 43 : $host.hashCode());
        result = result * PRIME + this.getPort();
        final Object $user = this.getUser();
        result = result * PRIME + ($user == null ? 43 : $user.hashCode());
        final Object $password = this.getPassword();
        result = result * PRIME + ($password == null ? 43 : $password.hashCode());
        result = result * PRIME + this.getInstances();
        return result;
    }

    public String toString() {
        return "GatewayServiceConfiguration(scheme=" + this.getScheme() + ", host=" + this.getHost() + ", port=" + this.getPort() + ", user=" + this.getUser() + ", password=" + this.getPassword() + ", instances=" + this.getInstances() + ")";
    }
}
