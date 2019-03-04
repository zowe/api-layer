/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.response.utils.config;

public class TlsConfiguration {
    private String keyAlias;
    private String keyPassword;
    private String keyStoreType;
    private String keyStore;
    private String keyStorePassword;
    private String trustStoreType;
    private String trustStore;
    private String trustStorePassword;

    @java.beans.ConstructorProperties({"keyAlias", "keyPassword", "keyStoreType", "keyStore", "keyStorePassword", "trustStoreType", "trustStore", "trustStorePassword"})
    public TlsConfiguration(String keyAlias, String keyPassword, String keyStoreType, String keyStore, String keyStorePassword, String trustStoreType, String trustStore, String trustStorePassword) {
        this.keyAlias = keyAlias;
        this.keyPassword = keyPassword;
        this.keyStoreType = keyStoreType;
        this.keyStore = keyStore;
        this.keyStorePassword = keyStorePassword;
        this.trustStoreType = trustStoreType;
        this.trustStore = trustStore;
        this.trustStorePassword = trustStorePassword;
    }

    public TlsConfiguration() {
    }

    public String getKeyAlias() {
        return this.keyAlias;
    }

    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    public String getKeyPassword() {
        return this.keyPassword;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public String getKeyStoreType() {
        return this.keyStoreType;
    }

    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

    public String getKeyStore() {
        return this.keyStore;
    }

    public void setKeyStore(String keyStore) {
        this.keyStore = keyStore;
    }

    public String getKeyStorePassword() {
        return this.keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getTrustStoreType() {
        return this.trustStoreType;
    }

    public void setTrustStoreType(String trustStoreType) {
        this.trustStoreType = trustStoreType;
    }

    public String getTrustStore() {
        return this.trustStore;
    }

    public void setTrustStore(String trustStore) {
        this.trustStore = trustStore;
    }

    public String getTrustStorePassword() {
        return this.trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof TlsConfiguration)) return false;
        final TlsConfiguration other = (TlsConfiguration) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$keyAlias = this.getKeyAlias();
        final Object other$keyAlias = other.getKeyAlias();
        if (this$keyAlias == null ? other$keyAlias != null : !this$keyAlias.equals(other$keyAlias)) return false;
        final Object this$keyPassword = this.getKeyPassword();
        final Object other$keyPassword = other.getKeyPassword();
        if (this$keyPassword == null ? other$keyPassword != null : !this$keyPassword.equals(other$keyPassword))
            return false;
        final Object this$keyStoreType = this.getKeyStoreType();
        final Object other$keyStoreType = other.getKeyStoreType();
        if (this$keyStoreType == null ? other$keyStoreType != null : !this$keyStoreType.equals(other$keyStoreType))
            return false;
        final Object this$keyStore = this.getKeyStore();
        final Object other$keyStore = other.getKeyStore();
        if (this$keyStore == null ? other$keyStore != null : !this$keyStore.equals(other$keyStore)) return false;
        final Object this$keyStorePassword = this.getKeyStorePassword();
        final Object other$keyStorePassword = other.getKeyStorePassword();
        if (this$keyStorePassword == null ? other$keyStorePassword != null : !this$keyStorePassword.equals(other$keyStorePassword))
            return false;
        final Object this$trustStoreType = this.getTrustStoreType();
        final Object other$trustStoreType = other.getTrustStoreType();
        if (this$trustStoreType == null ? other$trustStoreType != null : !this$trustStoreType.equals(other$trustStoreType))
            return false;
        final Object this$trustStore = this.getTrustStore();
        final Object other$trustStore = other.getTrustStore();
        if (this$trustStore == null ? other$trustStore != null : !this$trustStore.equals(other$trustStore))
            return false;
        final Object this$trustStorePassword = this.getTrustStorePassword();
        final Object other$trustStorePassword = other.getTrustStorePassword();
        if (this$trustStorePassword == null ? other$trustStorePassword != null : !this$trustStorePassword.equals(other$trustStorePassword))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof TlsConfiguration;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $keyAlias = this.getKeyAlias();
        result = result * PRIME + ($keyAlias == null ? 43 : $keyAlias.hashCode());
        final Object $keyPassword = this.getKeyPassword();
        result = result * PRIME + ($keyPassword == null ? 43 : $keyPassword.hashCode());
        final Object $keyStoreType = this.getKeyStoreType();
        result = result * PRIME + ($keyStoreType == null ? 43 : $keyStoreType.hashCode());
        final Object $keyStore = this.getKeyStore();
        result = result * PRIME + ($keyStore == null ? 43 : $keyStore.hashCode());
        final Object $keyStorePassword = this.getKeyStorePassword();
        result = result * PRIME + ($keyStorePassword == null ? 43 : $keyStorePassword.hashCode());
        final Object $trustStoreType = this.getTrustStoreType();
        result = result * PRIME + ($trustStoreType == null ? 43 : $trustStoreType.hashCode());
        final Object $trustStore = this.getTrustStore();
        result = result * PRIME + ($trustStore == null ? 43 : $trustStore.hashCode());
        final Object $trustStorePassword = this.getTrustStorePassword();
        result = result * PRIME + ($trustStorePassword == null ? 43 : $trustStorePassword.hashCode());
        return result;
    }

    public String toString() {
        return "TlsConfiguration(keyAlias=" + this.getKeyAlias() + ", keyPassword=" + this.getKeyPassword() + ", keyStoreType=" + this.getKeyStoreType() + ", keyStore=" + this.getKeyStore() + ", keyStorePassword=" + this.getKeyStorePassword() + ", trustStoreType=" + this.getTrustStoreType() + ", trustStore=" + this.getTrustStore() + ", trustStorePassword=" + this.getTrustStorePassword() + ")";
    }
}
