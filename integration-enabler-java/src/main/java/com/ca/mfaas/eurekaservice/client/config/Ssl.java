/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.eurekaservice.client.config;

public class Ssl {
    private boolean verifySslCertificatesOfServices;
    private String protocol;
    private String keyAlias;
    private String keyPassword;
    private String keyStore;
    private String keyStorePassword;
    private String keyStoreType;
    private String trustStore;
    private String trustStorePassword;
    private String trustStoreType;

    @java.beans.ConstructorProperties({"verifySslCertificatesOfServices", "protocol", "keyAlias", "keyPassword", "keyStore", "keyStorePassword", "keyStoreType", "trustStore", "trustStorePassword", "trustStoreType"})
    public Ssl(boolean verifySslCertificatesOfServices, String protocol, String keyAlias, String keyPassword, String keyStore, String keyStorePassword, String keyStoreType, String trustStore, String trustStorePassword, String trustStoreType) {
        this.verifySslCertificatesOfServices = verifySslCertificatesOfServices;
        this.protocol = protocol;
        this.keyAlias = keyAlias;
        this.keyPassword = keyPassword;
        this.keyStore = keyStore;
        this.keyStorePassword = keyStorePassword;
        this.keyStoreType = keyStoreType;
        this.trustStore = trustStore;
        this.trustStorePassword = trustStorePassword;
        this.trustStoreType = trustStoreType;
    }

    public Ssl() {
    }

    public boolean isVerifySslCertificatesOfServices() {
        return this.verifySslCertificatesOfServices;
    }

    public void setVerifySslCertificatesOfServices(boolean verifySslCertificatesOfServices) {
        this.verifySslCertificatesOfServices = verifySslCertificatesOfServices;
    }

    public String getProtocol() {
        return this.protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
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

    public String getKeyStoreType() {
        return this.keyStoreType;
    }

    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
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

    public String getTrustStoreType() {
        return this.trustStoreType;
    }

    public void setTrustStoreType(String trustStoreType) {
        this.trustStoreType = trustStoreType;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Ssl)) return false;
        final Ssl other = (Ssl) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.isVerifySslCertificatesOfServices() != other.isVerifySslCertificatesOfServices()) return false;
        final Object this$protocol = this.getProtocol();
        final Object other$protocol = other.getProtocol();
        if (this$protocol == null ? other$protocol != null : !this$protocol.equals(other$protocol)) return false;
        final Object this$keyAlias = this.getKeyAlias();
        final Object other$keyAlias = other.getKeyAlias();
        if (this$keyAlias == null ? other$keyAlias != null : !this$keyAlias.equals(other$keyAlias)) return false;
        final Object this$keyPassword = this.getKeyPassword();
        final Object other$keyPassword = other.getKeyPassword();
        if (this$keyPassword == null ? other$keyPassword != null : !this$keyPassword.equals(other$keyPassword))
            return false;
        final Object this$keyStore = this.getKeyStore();
        final Object other$keyStore = other.getKeyStore();
        if (this$keyStore == null ? other$keyStore != null : !this$keyStore.equals(other$keyStore)) return false;
        final Object this$keyStorePassword = this.getKeyStorePassword();
        final Object other$keyStorePassword = other.getKeyStorePassword();
        if (this$keyStorePassword == null ? other$keyStorePassword != null : !this$keyStorePassword.equals(other$keyStorePassword))
            return false;
        final Object this$keyStoreType = this.getKeyStoreType();
        final Object other$keyStoreType = other.getKeyStoreType();
        if (this$keyStoreType == null ? other$keyStoreType != null : !this$keyStoreType.equals(other$keyStoreType))
            return false;
        final Object this$trustStore = this.getTrustStore();
        final Object other$trustStore = other.getTrustStore();
        if (this$trustStore == null ? other$trustStore != null : !this$trustStore.equals(other$trustStore))
            return false;
        final Object this$trustStorePassword = this.getTrustStorePassword();
        final Object other$trustStorePassword = other.getTrustStorePassword();
        if (this$trustStorePassword == null ? other$trustStorePassword != null : !this$trustStorePassword.equals(other$trustStorePassword))
            return false;
        final Object this$trustStoreType = this.getTrustStoreType();
        final Object other$trustStoreType = other.getTrustStoreType();
        if (this$trustStoreType == null ? other$trustStoreType != null : !this$trustStoreType.equals(other$trustStoreType))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Ssl;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isVerifySslCertificatesOfServices() ? 79 : 97);
        final Object $protocol = this.getProtocol();
        result = result * PRIME + ($protocol == null ? 43 : $protocol.hashCode());
        final Object $keyAlias = this.getKeyAlias();
        result = result * PRIME + ($keyAlias == null ? 43 : $keyAlias.hashCode());
        final Object $keyPassword = this.getKeyPassword();
        result = result * PRIME + ($keyPassword == null ? 43 : $keyPassword.hashCode());
        final Object $keyStore = this.getKeyStore();
        result = result * PRIME + ($keyStore == null ? 43 : $keyStore.hashCode());
        final Object $keyStorePassword = this.getKeyStorePassword();
        result = result * PRIME + ($keyStorePassword == null ? 43 : $keyStorePassword.hashCode());
        final Object $keyStoreType = this.getKeyStoreType();
        result = result * PRIME + ($keyStoreType == null ? 43 : $keyStoreType.hashCode());
        final Object $trustStore = this.getTrustStore();
        result = result * PRIME + ($trustStore == null ? 43 : $trustStore.hashCode());
        final Object $trustStorePassword = this.getTrustStorePassword();
        result = result * PRIME + ($trustStorePassword == null ? 43 : $trustStorePassword.hashCode());
        final Object $trustStoreType = this.getTrustStoreType();
        result = result * PRIME + ($trustStoreType == null ? 43 : $trustStoreType.hashCode());
        return result;
    }

    public String toString() {
        return "Ssl(verifySslCertificatesOfServices=" + this.isVerifySslCertificatesOfServices() + ", protocol=" + this.getProtocol() + ", keyAlias=" + this.getKeyAlias() + ", keyPassword=" + this.getKeyPassword() + ", keyStore=" + this.getKeyStore() + ", keyStorePassword=" + this.getKeyStorePassword() + ", keyStoreType=" + this.getKeyStoreType() + ", trustStore=" + this.getTrustStore() + ", trustStorePassword=" + this.getTrustStorePassword() + ", trustStoreType=" + this.getTrustStoreType() + ")";
    }
}
