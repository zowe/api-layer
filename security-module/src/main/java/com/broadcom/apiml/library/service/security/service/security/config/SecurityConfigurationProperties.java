/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.security.service.security.config;

import org.slf4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "apiml.security", ignoreUnknownFields = false)
public class SecurityConfigurationProperties {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(SecurityConfigurationProperties.class);
    private String authenticationResponseTypeHeaderName = "Auth-Response-Type";
    private String loginPath = "/auth/login/**";
    private String queryPath = "/auth/query/**";
    private String logoutPath = "/auth/logout/**";
    private TokenProperties tokenProperties;
    private CookieProperties cookieProperties;
    private String zosmfServiceId;
    private boolean verifySslCertificatesOfServices = true;

    public SecurityConfigurationProperties() {
        this.cookieProperties = new CookieProperties();
        this.tokenProperties = new TokenProperties();
    }

    public String validatedZosmfServiceId() {
        if ((zosmfServiceId == null) || zosmfServiceId.isEmpty()) {
            log.error("z/OSMF service name not found. Set property apiml.security.zosmfServiceId to your service name.");
            throw new AuthenticationServiceException("Parameter 'zosmfServiceId' is not configured.");
        }
        return zosmfServiceId;
    }

    public String getAuthenticationResponseTypeHeaderName() {
        return this.authenticationResponseTypeHeaderName;
    }

    public void setAuthenticationResponseTypeHeaderName(String authenticationResponseTypeHeaderName) {
        this.authenticationResponseTypeHeaderName = authenticationResponseTypeHeaderName;
    }

    public String getLoginPath() {
        return this.loginPath;
    }

    public void setLoginPath(String loginPath) {
        this.loginPath = loginPath;
    }

    public String getQueryPath() {
        return this.queryPath;
    }

    public void setQueryPath(String queryPath) {
        this.queryPath = queryPath;
    }

    public String getLogoutPath() {
        return this.logoutPath;
    }

    public void setLogoutPath(String logoutPath) {
        this.logoutPath = logoutPath;
    }

    public TokenProperties getTokenProperties() {
        return this.tokenProperties;
    }

    public void setTokenProperties(TokenProperties tokenProperties) {
        this.tokenProperties = tokenProperties;
    }

    public CookieProperties getCookieProperties() {
        return this.cookieProperties;
    }

    public void setCookieProperties(CookieProperties cookieProperties) {
        this.cookieProperties = cookieProperties;
    }

    public String getZosmfServiceId() {
        return this.zosmfServiceId;
    }

    public void setZosmfServiceId(String zosmfServiceId) {
        this.zosmfServiceId = zosmfServiceId;
    }

    public boolean isVerifySslCertificatesOfServices() {
        return this.verifySslCertificatesOfServices;
    }

    public void setVerifySslCertificatesOfServices(boolean verifySslCertificatesOfServices) {
        this.verifySslCertificatesOfServices = verifySslCertificatesOfServices;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof SecurityConfigurationProperties)) return false;
        final SecurityConfigurationProperties other = (SecurityConfigurationProperties) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$authenticationResponseTypeHeaderName = this.getAuthenticationResponseTypeHeaderName();
        final Object other$authenticationResponseTypeHeaderName = other.getAuthenticationResponseTypeHeaderName();
        if (this$authenticationResponseTypeHeaderName == null ? other$authenticationResponseTypeHeaderName != null : !this$authenticationResponseTypeHeaderName.equals(other$authenticationResponseTypeHeaderName))
            return false;
        final Object this$loginPath = this.getLoginPath();
        final Object other$loginPath = other.getLoginPath();
        if (this$loginPath == null ? other$loginPath != null : !this$loginPath.equals(other$loginPath)) return false;
        final Object this$queryPath = this.getQueryPath();
        final Object other$queryPath = other.getQueryPath();
        if (this$queryPath == null ? other$queryPath != null : !this$queryPath.equals(other$queryPath)) return false;
        final Object this$logoutPath = this.getLogoutPath();
        final Object other$logoutPath = other.getLogoutPath();
        if (this$logoutPath == null ? other$logoutPath != null : !this$logoutPath.equals(other$logoutPath))
            return false;
        final Object this$tokenProperties = this.getTokenProperties();
        final Object other$tokenProperties = other.getTokenProperties();
        if (this$tokenProperties == null ? other$tokenProperties != null : !this$tokenProperties.equals(other$tokenProperties))
            return false;
        final Object this$cookieProperties = this.getCookieProperties();
        final Object other$cookieProperties = other.getCookieProperties();
        if (this$cookieProperties == null ? other$cookieProperties != null : !this$cookieProperties.equals(other$cookieProperties))
            return false;
        final Object this$zosmfServiceId = this.getZosmfServiceId();
        final Object other$zosmfServiceId = other.getZosmfServiceId();
        if (this$zosmfServiceId == null ? other$zosmfServiceId != null : !this$zosmfServiceId.equals(other$zosmfServiceId))
            return false;
        if (this.isVerifySslCertificatesOfServices() != other.isVerifySslCertificatesOfServices()) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof SecurityConfigurationProperties;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $authenticationResponseTypeHeaderName = this.getAuthenticationResponseTypeHeaderName();
        result = result * PRIME + ($authenticationResponseTypeHeaderName == null ? 43 : $authenticationResponseTypeHeaderName.hashCode());
        final Object $loginPath = this.getLoginPath();
        result = result * PRIME + ($loginPath == null ? 43 : $loginPath.hashCode());
        final Object $queryPath = this.getQueryPath();
        result = result * PRIME + ($queryPath == null ? 43 : $queryPath.hashCode());
        final Object $logoutPath = this.getLogoutPath();
        result = result * PRIME + ($logoutPath == null ? 43 : $logoutPath.hashCode());
        final Object $tokenProperties = this.getTokenProperties();
        result = result * PRIME + ($tokenProperties == null ? 43 : $tokenProperties.hashCode());
        final Object $cookieProperties = this.getCookieProperties();
        result = result * PRIME + ($cookieProperties == null ? 43 : $cookieProperties.hashCode());
        final Object $zosmfServiceId = this.getZosmfServiceId();
        result = result * PRIME + ($zosmfServiceId == null ? 43 : $zosmfServiceId.hashCode());
        result = result * PRIME + (this.isVerifySslCertificatesOfServices() ? 79 : 97);
        return result;
    }

    public String toString() {
        return "SecurityConfigurationProperties(authenticationResponseTypeHeaderName=" + this.getAuthenticationResponseTypeHeaderName() + ", loginPath=" + this.getLoginPath() + ", queryPath=" + this.getQueryPath() + ", logoutPath=" + this.getLogoutPath() + ", tokenProperties=" + this.getTokenProperties() + ", cookieProperties=" + this.getCookieProperties() + ", zosmfServiceId=" + this.getZosmfServiceId() + ", verifySslCertificatesOfServices=" + this.isVerifySslCertificatesOfServices() + ")";
    }

    public static class TokenProperties {
        private String authorizationHeader = "Authorization";
        private String bearerPrefix = "Bearer ";
        private long expirationInSeconds = 24 * 60 * 60;
        private String issuer = "APIML";
        private String shortTtlUsername = "expire";
        private long shortTtlExpirationInSeconds = 1;

        public TokenProperties() {
        }

        public String getAuthorizationHeader() {
            return this.authorizationHeader;
        }

        public void setAuthorizationHeader(String authorizationHeader) {
            this.authorizationHeader = authorizationHeader;
        }

        public String getBearerPrefix() {
            return this.bearerPrefix;
        }

        public void setBearerPrefix(String bearerPrefix) {
            this.bearerPrefix = bearerPrefix;
        }

        public long getExpirationInSeconds() {
            return this.expirationInSeconds;
        }

        public void setExpirationInSeconds(long expirationInSeconds) {
            this.expirationInSeconds = expirationInSeconds;
        }

        public String getIssuer() {
            return this.issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getShortTtlUsername() {
            return this.shortTtlUsername;
        }

        public void setShortTtlUsername(String shortTtlUsername) {
            this.shortTtlUsername = shortTtlUsername;
        }

        public long getShortTtlExpirationInSeconds() {
            return this.shortTtlExpirationInSeconds;
        }

        public void setShortTtlExpirationInSeconds(long shortTtlExpirationInSeconds) {
            this.shortTtlExpirationInSeconds = shortTtlExpirationInSeconds;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof TokenProperties)) return false;
            final TokenProperties other = (TokenProperties) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$authorizationHeader = this.getAuthorizationHeader();
            final Object other$authorizationHeader = other.getAuthorizationHeader();
            if (this$authorizationHeader == null ? other$authorizationHeader != null : !this$authorizationHeader.equals(other$authorizationHeader))
                return false;
            final Object this$bearerPrefix = this.getBearerPrefix();
            final Object other$bearerPrefix = other.getBearerPrefix();
            if (this$bearerPrefix == null ? other$bearerPrefix != null : !this$bearerPrefix.equals(other$bearerPrefix))
                return false;
            if (this.getExpirationInSeconds() != other.getExpirationInSeconds()) return false;
            final Object this$issuer = this.getIssuer();
            final Object other$issuer = other.getIssuer();
            if (this$issuer == null ? other$issuer != null : !this$issuer.equals(other$issuer)) return false;
            final Object this$shortTtlUsername = this.getShortTtlUsername();
            final Object other$shortTtlUsername = other.getShortTtlUsername();
            if (this$shortTtlUsername == null ? other$shortTtlUsername != null : !this$shortTtlUsername.equals(other$shortTtlUsername))
                return false;
            if (this.getShortTtlExpirationInSeconds() != other.getShortTtlExpirationInSeconds()) return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof TokenProperties;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $authorizationHeader = this.getAuthorizationHeader();
            result = result * PRIME + ($authorizationHeader == null ? 43 : $authorizationHeader.hashCode());
            final Object $bearerPrefix = this.getBearerPrefix();
            result = result * PRIME + ($bearerPrefix == null ? 43 : $bearerPrefix.hashCode());
            final long $expirationInSeconds = this.getExpirationInSeconds();
            result = result * PRIME + (int) ($expirationInSeconds >>> 32 ^ $expirationInSeconds);
            final Object $issuer = this.getIssuer();
            result = result * PRIME + ($issuer == null ? 43 : $issuer.hashCode());
            final Object $shortTtlUsername = this.getShortTtlUsername();
            result = result * PRIME + ($shortTtlUsername == null ? 43 : $shortTtlUsername.hashCode());
            final long $shortTtlExpirationInSeconds = this.getShortTtlExpirationInSeconds();
            result = result * PRIME + (int) ($shortTtlExpirationInSeconds >>> 32 ^ $shortTtlExpirationInSeconds);
            return result;
        }

        public String toString() {
            return "SecurityConfigurationProperties.TokenProperties(authorizationHeader=" + this.getAuthorizationHeader() + ", bearerPrefix=" + this.getBearerPrefix() + ", expirationInSeconds=" + this.getExpirationInSeconds() + ", issuer=" + this.getIssuer() + ", shortTtlUsername=" + this.getShortTtlUsername() + ", shortTtlExpirationInSeconds=" + this.getShortTtlExpirationInSeconds() + ")";
        }
    }

    public static class CookieProperties {
        private String cookieName = "apimlAuthenticationToken";
        private boolean cookieSecure = true;
        private String cookiePath = "/";
        private String cookieComment = "API Mediation Layer security token";
        private Integer cookieMaxAge = 24 * 60 * 60;

        public CookieProperties() {
        }

        public String getCookieName() {
            return this.cookieName;
        }

        public void setCookieName(String cookieName) {
            this.cookieName = cookieName;
        }

        public boolean isCookieSecure() {
            return this.cookieSecure;
        }

        public void setCookieSecure(boolean cookieSecure) {
            this.cookieSecure = cookieSecure;
        }

        public String getCookiePath() {
            return this.cookiePath;
        }

        public void setCookiePath(String cookiePath) {
            this.cookiePath = cookiePath;
        }

        public String getCookieComment() {
            return this.cookieComment;
        }

        public void setCookieComment(String cookieComment) {
            this.cookieComment = cookieComment;
        }

        public Integer getCookieMaxAge() {
            return this.cookieMaxAge;
        }

        public void setCookieMaxAge(Integer cookieMaxAge) {
            this.cookieMaxAge = cookieMaxAge;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof CookieProperties)) return false;
            final CookieProperties other = (CookieProperties) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$cookieName = this.getCookieName();
            final Object other$cookieName = other.getCookieName();
            if (this$cookieName == null ? other$cookieName != null : !this$cookieName.equals(other$cookieName))
                return false;
            if (this.isCookieSecure() != other.isCookieSecure()) return false;
            final Object this$cookiePath = this.getCookiePath();
            final Object other$cookiePath = other.getCookiePath();
            if (this$cookiePath == null ? other$cookiePath != null : !this$cookiePath.equals(other$cookiePath))
                return false;
            final Object this$cookieComment = this.getCookieComment();
            final Object other$cookieComment = other.getCookieComment();
            if (this$cookieComment == null ? other$cookieComment != null : !this$cookieComment.equals(other$cookieComment))
                return false;
            final Object this$cookieMaxAge = this.getCookieMaxAge();
            final Object other$cookieMaxAge = other.getCookieMaxAge();
            if (this$cookieMaxAge == null ? other$cookieMaxAge != null : !this$cookieMaxAge.equals(other$cookieMaxAge))
                return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof CookieProperties;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $cookieName = this.getCookieName();
            result = result * PRIME + ($cookieName == null ? 43 : $cookieName.hashCode());
            result = result * PRIME + (this.isCookieSecure() ? 79 : 97);
            final Object $cookiePath = this.getCookiePath();
            result = result * PRIME + ($cookiePath == null ? 43 : $cookiePath.hashCode());
            final Object $cookieComment = this.getCookieComment();
            result = result * PRIME + ($cookieComment == null ? 43 : $cookieComment.hashCode());
            final Object $cookieMaxAge = this.getCookieMaxAge();
            result = result * PRIME + ($cookieMaxAge == null ? 43 : $cookieMaxAge.hashCode());
            return result;
        }

        public String toString() {
            return "SecurityConfigurationProperties.CookieProperties(cookieName=" + this.getCookieName() + ", cookieSecure=" + this.isCookieSecure() + ", cookiePath=" + this.getCookiePath() + ", cookieComment=" + this.getCookieComment() + ", cookieMaxAge=" + this.getCookieMaxAge() + ")";
        }
    }
}
