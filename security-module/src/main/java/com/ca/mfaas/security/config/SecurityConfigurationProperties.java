/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.security.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Component;

/**
 * General configuration of security constants: endpoints, cookie, token
 */
@Data
@Component
@Slf4j
@ConfigurationProperties(prefix = "apiml.security.auth", ignoreUnknownFields = false)
public class SecurityConfigurationProperties {
    public SecurityConfigurationProperties() {
        this.cookieProperties = new CookieProperties();
        this.tokenProperties = new TokenProperties();
    }

    private String authenticationResponseTypeHeaderName = "Auth-Response-Type";
    private String loginPath = "/auth/login/**";
    private String queryPath = "/auth/query/**";
    private String logoutPath = "/auth/logout/**";
    private TokenProperties tokenProperties;
    private CookieProperties cookieProperties;
    private String zosmfServiceId;
    private boolean verifySslCertificatesOfServices = true;
    private String ciphers = null;
    private String signatureAlgorithm;
    private String secretKey = null;

    @Data
    public static class TokenProperties {
        private String authorizationHeader = "Authorization";
        private String bearerPrefix = "Bearer ";
        private long expirationInSeconds = 24 * 60 * 60;
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

    /**
     * Return the zosmf service id if it is set
     * @throws AuthenticationServiceException if the zosmf service id is not configured
     * @return the zosmf service id
     */
    public String validatedZosmfServiceId() {
        if ((zosmfServiceId == null) || zosmfServiceId.isEmpty()) {
            log.error("z/OSMF service name not found. Set property apiml.security.auth.zosmfServiceId to your service name.");
            throw new AuthenticationServiceException("Parameter 'zosmfServiceId' is not configured.");
        }
        return zosmfServiceId;
    }
}
