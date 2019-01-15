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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "apiml.security", ignoreUnknownFields = false)
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
}
