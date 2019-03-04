/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.tls;

public class HttpsConfig {
    private String protocol = "TLSv1.2";
    private String trustStore = null;
    private String trustStorePassword = null;
    private String trustStoreType = "PKCS12";
    private boolean trustStoreRequired = false;
    private String keyAlias = null;
    private String keyStore = null;
    private String keyStorePassword = null;
    private String keyPassword = null;
    private String keyStoreType = "PKCS12";
    private boolean clientAuth = false;
    private boolean verifySslCertificatesOfServices = true;

    @java.beans.ConstructorProperties({"protocol", "trustStore", "trustStorePassword", "trustStoreType", "trustStoreRequired", "keyAlias", "keyStore", "keyStorePassword", "keyPassword", "keyStoreType", "clientAuth", "verifySslCertificatesOfServices"})
    HttpsConfig(String protocol, String trustStore, String trustStorePassword, String trustStoreType, boolean trustStoreRequired, String keyAlias, String keyStore, String keyStorePassword, String keyPassword, String keyStoreType, boolean clientAuth, boolean verifySslCertificatesOfServices) {
        this.protocol = protocol;
        this.trustStore = trustStore;
        this.trustStorePassword = trustStorePassword;
        this.trustStoreType = trustStoreType;
        this.trustStoreRequired = trustStoreRequired;
        this.keyAlias = keyAlias;
        this.keyStore = keyStore;
        this.keyStorePassword = keyStorePassword;
        this.keyPassword = keyPassword;
        this.keyStoreType = keyStoreType;
        this.clientAuth = clientAuth;
        this.verifySslCertificatesOfServices = verifySslCertificatesOfServices;
    }

    public static HttpsConfigBuilder builder() {
        return new HttpsConfigBuilder();
    }

    public String getProtocol() {
        return this.protocol;
    }

    public String getTrustStore() {
        return this.trustStore;
    }

    public String getTrustStorePassword() {
        return this.trustStorePassword;
    }

    public String getTrustStoreType() {
        return this.trustStoreType;
    }

    public boolean isTrustStoreRequired() {
        return this.trustStoreRequired;
    }

    public String getKeyAlias() {
        return this.keyAlias;
    }

    public String getKeyStore() {
        return this.keyStore;
    }

    public String getKeyStorePassword() {
        return this.keyStorePassword;
    }

    public String getKeyPassword() {
        return this.keyPassword;
    }

    public String getKeyStoreType() {
        return this.keyStoreType;
    }

    public boolean isClientAuth() {
        return this.clientAuth;
    }

    public boolean isVerifySslCertificatesOfServices() {
        return this.verifySslCertificatesOfServices;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof HttpsConfig)) return false;
        final HttpsConfig other = (HttpsConfig) o;
        final Object this$protocol = this.getProtocol();
        final Object other$protocol = other.getProtocol();
        if (this$protocol == null ? other$protocol != null : !this$protocol.equals(other$protocol)) return false;
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
        if (this.isTrustStoreRequired() != other.isTrustStoreRequired()) return false;
        final Object this$keyAlias = this.getKeyAlias();
        final Object other$keyAlias = other.getKeyAlias();
        if (this$keyAlias == null ? other$keyAlias != null : !this$keyAlias.equals(other$keyAlias)) return false;
        final Object this$keyStore = this.getKeyStore();
        final Object other$keyStore = other.getKeyStore();
        if (this$keyStore == null ? other$keyStore != null : !this$keyStore.equals(other$keyStore)) return false;
        final Object this$keyStorePassword = this.getKeyStorePassword();
        final Object other$keyStorePassword = other.getKeyStorePassword();
        if (this$keyStorePassword == null ? other$keyStorePassword != null : !this$keyStorePassword.equals(other$keyStorePassword))
            return false;
        final Object this$keyPassword = this.getKeyPassword();
        final Object other$keyPassword = other.getKeyPassword();
        if (this$keyPassword == null ? other$keyPassword != null : !this$keyPassword.equals(other$keyPassword))
            return false;
        final Object this$keyStoreType = this.getKeyStoreType();
        final Object other$keyStoreType = other.getKeyStoreType();
        if (this$keyStoreType == null ? other$keyStoreType != null : !this$keyStoreType.equals(other$keyStoreType))
            return false;
        if (this.isClientAuth() != other.isClientAuth()) return false;
        if (this.isVerifySslCertificatesOfServices() != other.isVerifySslCertificatesOfServices()) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $protocol = this.getProtocol();
        result = result * PRIME + ($protocol == null ? 43 : $protocol.hashCode());
        final Object $trustStore = this.getTrustStore();
        result = result * PRIME + ($trustStore == null ? 43 : $trustStore.hashCode());
        final Object $trustStorePassword = this.getTrustStorePassword();
        result = result * PRIME + ($trustStorePassword == null ? 43 : $trustStorePassword.hashCode());
        final Object $trustStoreType = this.getTrustStoreType();
        result = result * PRIME + ($trustStoreType == null ? 43 : $trustStoreType.hashCode());
        result = result * PRIME + (this.isTrustStoreRequired() ? 79 : 97);
        final Object $keyAlias = this.getKeyAlias();
        result = result * PRIME + ($keyAlias == null ? 43 : $keyAlias.hashCode());
        final Object $keyStore = this.getKeyStore();
        result = result * PRIME + ($keyStore == null ? 43 : $keyStore.hashCode());
        final Object $keyStorePassword = this.getKeyStorePassword();
        result = result * PRIME + ($keyStorePassword == null ? 43 : $keyStorePassword.hashCode());
        final Object $keyPassword = this.getKeyPassword();
        result = result * PRIME + ($keyPassword == null ? 43 : $keyPassword.hashCode());
        final Object $keyStoreType = this.getKeyStoreType();
        result = result * PRIME + ($keyStoreType == null ? 43 : $keyStoreType.hashCode());
        result = result * PRIME + (this.isClientAuth() ? 79 : 97);
        result = result * PRIME + (this.isVerifySslCertificatesOfServices() ? 79 : 97);
        return result;
    }

    public String toString() {
        return "HttpsConfig(protocol=" + this.getProtocol() + ", trustStore=" + this.getTrustStore() + ", trustStoreType=" + this.getTrustStoreType() + ", trustStoreRequired=" + this.isTrustStoreRequired() + ", keyAlias=" + this.getKeyAlias() + ", keyStore=" + this.getKeyStore() + ", keyStoreType=" + this.getKeyStoreType() + ", clientAuth=" + this.isClientAuth() + ", verifySslCertificatesOfServices=" + this.isVerifySslCertificatesOfServices() + ")";
    }

    public static class HttpsConfigBuilder {
        private String protocol;
        private String trustStore;
        private String trustStorePassword;
        private String trustStoreType;
        private boolean trustStoreRequired;
        private String keyAlias;
        private String keyStore;
        private String keyStorePassword;
        private String keyPassword;
        private String keyStoreType;
        private boolean clientAuth;
        private boolean verifySslCertificatesOfServices;

        HttpsConfigBuilder() {
        }

        public HttpsConfig.HttpsConfigBuilder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public HttpsConfig.HttpsConfigBuilder trustStore(String trustStore) {
            this.trustStore = trustStore;
            return this;
        }

        public HttpsConfig.HttpsConfigBuilder trustStorePassword(String trustStorePassword) {
            this.trustStorePassword = trustStorePassword;
            return this;
        }

        public HttpsConfig.HttpsConfigBuilder trustStoreType(String trustStoreType) {
            this.trustStoreType = trustStoreType;
            return this;
        }

        public HttpsConfig.HttpsConfigBuilder trustStoreRequired(boolean trustStoreRequired) {
            this.trustStoreRequired = trustStoreRequired;
            return this;
        }

        public HttpsConfig.HttpsConfigBuilder keyAlias(String keyAlias) {
            this.keyAlias = keyAlias;
            return this;
        }

        public HttpsConfig.HttpsConfigBuilder keyStore(String keyStore) {
            this.keyStore = keyStore;
            return this;
        }

        public HttpsConfig.HttpsConfigBuilder keyStorePassword(String keyStorePassword) {
            this.keyStorePassword = keyStorePassword;
            return this;
        }

        public HttpsConfig.HttpsConfigBuilder keyPassword(String keyPassword) {
            this.keyPassword = keyPassword;
            return this;
        }

        public HttpsConfig.HttpsConfigBuilder keyStoreType(String keyStoreType) {
            this.keyStoreType = keyStoreType;
            return this;
        }

        public HttpsConfig.HttpsConfigBuilder clientAuth(boolean clientAuth) {
            this.clientAuth = clientAuth;
            return this;
        }

        public HttpsConfig.HttpsConfigBuilder verifySslCertificatesOfServices(boolean verifySslCertificatesOfServices) {
            this.verifySslCertificatesOfServices = verifySslCertificatesOfServices;
            return this;
        }

        public HttpsConfig build() {
            return new HttpsConfig(protocol, trustStore, trustStorePassword, trustStoreType, trustStoreRequired, keyAlias, keyStore, keyStorePassword, keyPassword, keyStoreType, clientAuth, verifySslCertificatesOfServices);
        }

        public String toString() {
            return "HttpsConfig.HttpsConfigBuilder(protocol=" + this.protocol + ", trustStore=" + this.trustStore + ", trustStorePassword=" + this.trustStorePassword + ", trustStoreType=" + this.trustStoreType + ", trustStoreRequired=" + this.trustStoreRequired + ", keyAlias=" + this.keyAlias + ", keyStore=" + this.keyStore + ", keyStorePassword=" + this.keyStorePassword + ", keyPassword=" + this.keyPassword + ", keyStoreType=" + this.keyStoreType + ", clientAuth=" + this.clientAuth + ", verifySslCertificatesOfServices=" + this.verifySslCertificatesOfServices + ")";
        }
    }
}
