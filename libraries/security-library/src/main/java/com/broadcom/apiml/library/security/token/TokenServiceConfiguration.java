/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.security.token;

public class TokenServiceConfiguration {
    private final String secret;
    private final long expirationInSeconds;
    private final String issuer;
    private final String shortTtlUsername;
    private final long shortTtlExpiration;

    @java.beans.ConstructorProperties({"secret", "expirationInSeconds", "issuer", "shortTtlUsername", "shortTtlExpiration"})
    TokenServiceConfiguration(String secret, long expirationInSeconds, String issuer, String shortTtlUsername, long shortTtlExpiration) {
        this.secret = secret;
        this.expirationInSeconds = expirationInSeconds;
        this.issuer = issuer;
        this.shortTtlUsername = shortTtlUsername;
        this.shortTtlExpiration = shortTtlExpiration;
    }

    public static TokenServiceConfigurationBuilder builder() {
        return new TokenServiceConfigurationBuilder();
    }

    public String getSecret() {
        return this.secret;
    }

    public long getExpirationInSeconds() {
        return this.expirationInSeconds;
    }

    public String getIssuer() {
        return this.issuer;
    }

    public String getShortTtlUsername() {
        return this.shortTtlUsername;
    }

    public long getShortTtlExpiration() {
        return this.shortTtlExpiration;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof TokenServiceConfiguration)) return false;
        final TokenServiceConfiguration other = (TokenServiceConfiguration) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$secret = this.getSecret();
        final Object other$secret = other.getSecret();
        if (this$secret == null ? other$secret != null : !this$secret.equals(other$secret)) return false;
        if (this.getExpirationInSeconds() != other.getExpirationInSeconds()) return false;
        final Object this$issuer = this.getIssuer();
        final Object other$issuer = other.getIssuer();
        if (this$issuer == null ? other$issuer != null : !this$issuer.equals(other$issuer)) return false;
        final Object this$shortTtlUsername = this.getShortTtlUsername();
        final Object other$shortTtlUsername = other.getShortTtlUsername();
        if (this$shortTtlUsername == null ? other$shortTtlUsername != null : !this$shortTtlUsername.equals(other$shortTtlUsername))
            return false;
        if (this.getShortTtlExpiration() != other.getShortTtlExpiration()) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof TokenServiceConfiguration;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $secret = this.getSecret();
        result = result * PRIME + ($secret == null ? 43 : $secret.hashCode());
        final long $expirationInSeconds = this.getExpirationInSeconds();
        result = result * PRIME + (int) ($expirationInSeconds >>> 32 ^ $expirationInSeconds);
        final Object $issuer = this.getIssuer();
        result = result * PRIME + ($issuer == null ? 43 : $issuer.hashCode());
        final Object $shortTtlUsername = this.getShortTtlUsername();
        result = result * PRIME + ($shortTtlUsername == null ? 43 : $shortTtlUsername.hashCode());
        final long $shortTtlExpiration = this.getShortTtlExpiration();
        result = result * PRIME + (int) ($shortTtlExpiration >>> 32 ^ $shortTtlExpiration);
        return result;
    }

    public String toString() {
        return "TokenServiceConfiguration(secret=" + this.getSecret() + ", expirationInSeconds=" + this.getExpirationInSeconds() + ", issuer=" + this.getIssuer() + ", shortTtlUsername=" + this.getShortTtlUsername() + ", shortTtlExpiration=" + this.getShortTtlExpiration() + ")";
    }

    public TokenServiceConfigurationBuilder toBuilder() {
        return new TokenServiceConfigurationBuilder().secret(this.secret).expirationInSeconds(this.expirationInSeconds).issuer(this.issuer).shortTtlUsername(this.shortTtlUsername).shortTtlExpiration(this.shortTtlExpiration);
    }

    public static class TokenServiceConfigurationBuilder {
        private String secret;
        private long expirationInSeconds;
        private String issuer;
        private String shortTtlUsername;
        private long shortTtlExpiration;

        TokenServiceConfigurationBuilder() {
        }

        public TokenServiceConfiguration.TokenServiceConfigurationBuilder secret(String secret) {
            this.secret = secret;
            return this;
        }

        public TokenServiceConfiguration.TokenServiceConfigurationBuilder expirationInSeconds(long expirationInSeconds) {
            this.expirationInSeconds = expirationInSeconds;
            return this;
        }

        public TokenServiceConfiguration.TokenServiceConfigurationBuilder issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public TokenServiceConfiguration.TokenServiceConfigurationBuilder shortTtlUsername(String shortTtlUsername) {
            this.shortTtlUsername = shortTtlUsername;
            return this;
        }

        public TokenServiceConfiguration.TokenServiceConfigurationBuilder shortTtlExpiration(long shortTtlExpiration) {
            this.shortTtlExpiration = shortTtlExpiration;
            return this;
        }

        public TokenServiceConfiguration build() {
            return new TokenServiceConfiguration(secret, expirationInSeconds, issuer, shortTtlUsername, shortTtlExpiration);
        }

        public String toString() {
            return "TokenServiceConfiguration.TokenServiceConfigurationBuilder(secret=" + this.secret + ", expirationInSeconds=" + this.expirationInSeconds + ", issuer=" + this.issuer + ", shortTtlUsername=" + this.shortTtlUsername + ", shortTtlExpiration=" + this.shortTtlExpiration + ")";
        }
    }
}
