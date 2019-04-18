/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Component;

@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "apiml.security", ignoreUnknownFields = false)
@SuppressWarnings("squid:S1075") //Suppress because endpoints are okay
public class SecurityConfigurationProperties {
    private String loginPath = "/api/v1/gateway/auth/login/**";
    private String queryPath = "/api/v1/gateway/auth/query/**";
    private TokenProperties tokenProperties;
    private CookieProperties cookieProperties;
    private String zosmfServiceId;
    private String authProvider = "zosmf";
    private boolean verifySslCertificatesOfServices = true;

    @Data
    public static class TokenProperties {
        private int expirationInSeconds = 24 * 60 * 60;
        private String issuer = "APIML";
        private String shortTtlUsername = "expire";
        private long shortTtlExpirationInSeconds = 1;
    }

    @Data
    public static class CookieProperties {
        private String cookieName = "apimlAuthenticationToken";
        private boolean cookieSecure = true;
        private String cookiePath = "/";
        private String cookieComment = "API Mediation Layer security token";
        private Integer cookieMaxAge = 24 * 60 * 60;
    }

    public SecurityConfigurationProperties() {
        this.cookieProperties = new CookieProperties();
        this.tokenProperties = new TokenProperties();
    }

    /**
     * Return the zosmf service id if it is set
     * @throws AuthenticationServiceException if the zosmf service id is not configured
     * @return the zosmf service id
     */
    public String validatedZosmfServiceId() {
        if ((zosmfServiceId == null) || zosmfServiceId.isEmpty()) {
            log.error("z/OSMF service name not found. Set property apiml.security.zosmfServiceId to your service name.");
            throw new AuthenticationServiceException("Parameter 'zosmfServiceId' is not configured.");
        }
        return zosmfServiceId;
    }
}
