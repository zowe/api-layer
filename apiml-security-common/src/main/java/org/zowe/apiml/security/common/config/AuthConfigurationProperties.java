/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.config;

import lombok.Data;
import org.apache.tomcat.util.http.SameSiteCookies;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Component;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.SecurityUtils;


/**
 * Configuration class for authentication-related security properties
 */
@Data
@Component
@ConfigurationProperties(prefix = "apiml.security.auth", ignoreUnknownFields = false)
public class AuthConfigurationProperties {

    @InjectApimlLogger
    private ApimlLogger apimlLog = ApimlLogger.empty();

    // General properties
    private String gatewayLoginEndpoint = "/gateway/api/v1/auth/login";
    private String gatewayLogoutEndpoint = "/gateway/api/v1/auth/logout";
    private String gatewayQueryEndpoint = "/gateway/api/v1/auth/query";
    private String gatewayTicketEndpoint = "/gateway/api/v1/auth/ticket";

    private String zaasLoginEndpoint = "/zaas/api/v1/auth/login";
    private String zaasLogoutEndpoint = "/zaas/api/v1/auth/logout";
    private String zaasQueryEndpoint = "/zaas/api/v1/auth/query";
    private String zaasTicketEndpoint = "/zaas/api/v1/auth/ticket";

    private String gatewayAccessTokenEndpoint = "/gateway/api/v1/auth/access-token/generate";
    private String zaasAccessTokenEndpoint = "/zaas/api/v1/auth/access-token/generate";

    private String gatewayRevokeMultipleAccessTokens = "/gateway/auth/access-token/revoke/tokens";
    private String zaasRevokeMultipleAccessTokens = "/zaas/api/v1/auth/access-token/revoke/tokens";

    private String gatewayEvictAccessTokensAndRules = "/gateway/auth/access-token/evict";
    private String zaasEvictAccessTokensAndRules = "/zaas/api/v1/auth/access-token/evict";

    private String gatewayRefreshEndpoint = "/gateway/api/v1/auth/refresh";
    private String zaasRefreshEndpoint = "/zaas/api/v1/auth/refresh";

    private String serviceLoginEndpoint = "/auth/login";
    private String serviceLogoutEndpoint = "/auth/logout";

    private AuthConfigurationProperties.TokenProperties tokenProperties;
    private AuthConfigurationProperties.CookieProperties cookieProperties;

    private String provider = "zosmf";
    private AuthConfigurationProperties.X509Cert x509Cert;

    private AuthConfigurationProperties.Zosmf zosmf = new AuthConfigurationProperties.Zosmf();

    private AuthConfigurationProperties.JwtAuthProperties jwt = new AuthConfigurationProperties.JwtAuthProperties();
    private AuthConfigurationProperties.PassTicketAuthProperties passTicket = new AuthConfigurationProperties.PassTicketAuthProperties();

    public enum JWT_AUTOCONFIGURATION_MODE {
        LTPA,
        JWT
    }

    //JWT Custom Header property
    @Data
    public static class JwtAuthProperties {
        private String customAuthHeader;
    }

    //PassTicket Custom Headers properties
    @Data
    public static class PassTicketAuthProperties {
        private String customUserHeader;
        private String customAuthHeader;
    }

    //Token properties
    @Data
    public static class TokenProperties {
        private int expirationInSeconds = 8 * 60 * 60;
        private String issuer = "APIML";
        private String shortTtlUsername = "expire";
        private long shortTtlExpirationInSeconds = 50;
    }

    //Cookie properties
    @Data
    public static class CookieProperties {
        private String cookieName = SecurityUtils.COOKIE_AUTH_NAME;
        private String cookieNamePAT = ApimlConstants.PAT_COOKIE_AUTH_NAME;
        private boolean cookieSecure = true;
        private String cookiePath = "/";
        private Integer cookieMaxAge = null;
        private SameSiteCookies cookieSameSite = SameSiteCookies.STRICT;
    }

    @Data
    public static class X509Cert {
        private Integer timeout = 15 * 60;
    }

    @Data
    public static class Zosmf {
        private String serviceId;
        private String jwtEndpoint = "/jwt/ibm/api/zOSMFBuilder/jwk";
        private JWT_AUTOCONFIGURATION_MODE jwtAutoconfiguration = JWT_AUTOCONFIGURATION_MODE.JWT;
    }

    public AuthConfigurationProperties() {
        this.cookieProperties = new AuthConfigurationProperties.CookieProperties();
        this.tokenProperties = new AuthConfigurationProperties.TokenProperties();
        this.x509Cert = new AuthConfigurationProperties.X509Cert();
    }

    /**
     * Return the z/OSMF service id when it is set
     *
     * @return the z/OSMF service id
     * @throws AuthenticationServiceException if the z/OSMF service id is not configured
     */
    public String validatedZosmfServiceId() {
        if (provider.equalsIgnoreCase(AuthenticationScheme.ZOSMF.getScheme())
            && ((zosmf.getServiceId() == null) || zosmf.getServiceId().isEmpty())) {
            apimlLog.log("org.zowe.apiml.security.zosmfNotFound");
            throw new AuthenticationServiceException("The parameter 'apiml.security.auth.zosmf.serviceId' is not configured.");
        }
        return zosmf.getServiceId();
    }
}
