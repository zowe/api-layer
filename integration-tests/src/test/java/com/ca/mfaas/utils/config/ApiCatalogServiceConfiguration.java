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

public class ApiCatalogServiceConfiguration {
    private String user;
    private String password;

    @java.beans.ConstructorProperties({"user", "password"})
    public ApiCatalogServiceConfiguration(String user, String password) {
        this.user = user;
        this.password = password;
    }

    public ApiCatalogServiceConfiguration() {
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

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ApiCatalogServiceConfiguration)) return false;
        final ApiCatalogServiceConfiguration other = (ApiCatalogServiceConfiguration) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$user = this.getUser();
        final Object other$user = other.getUser();
        if (this$user == null ? other$user != null : !this$user.equals(other$user)) return false;
        final Object this$password = this.getPassword();
        final Object other$password = other.getPassword();
        if (this$password == null ? other$password != null : !this$password.equals(other$password)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ApiCatalogServiceConfiguration;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $user = this.getUser();
        result = result * PRIME + ($user == null ? 43 : $user.hashCode());
        final Object $password = this.getPassword();
        result = result * PRIME + ($password == null ? 43 : $password.hashCode());
        return result;
    }

    public String toString() {
        return "ApiCatalogServiceConfiguration(user=" + this.getUser() + ", password=" + this.getPassword() + ")";
    }
}
