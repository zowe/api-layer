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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Component;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;


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
    private static final String API_PREFIX = "/api/v1/gateway";

    private final String serviceLoginEndpoint = "/auth/login";
    private final String serviceLogoutEndpoint = "/auth/logout";
    private final String serviceQueryEndpoint = "/auth/query";
    private final String serviceTicketEndpoint = "/auth/ticket";

    private final String gatewayLoginEndpoint = API_PREFIX + serviceLoginEndpoint;
    private final String gatewayLogoutEndpoint = API_PREFIX + serviceLogoutEndpoint;
    private final String gatewayQueryEndpoint = API_PREFIX + serviceQueryEndpoint;
    private final String gatewayTicketEndpoint = API_PREFIX + serviceTicketEndpoint;

    private AuthConfigurationProperties.TokenProperties tokenProperties;
    private AuthConfigurationProperties.CookieProperties cookieProperties;

    private String zosmfServiceId;
    private String provider = "zosmf";

    private AuthConfigurationProperties.PassTicket passTicket;

    private String jwtKeyAlias;

    //Token properties
    @Data
    public static class TokenProperties {
        private int expirationInSeconds = 24 * 60 * 60;
        private String issuer = "APIML";
        private String shortTtlUsername = "expire";
        private long shortTtlExpirationInSeconds = 1;
    }

    //Cookie properties
    @Data
    public static class CookieProperties {
        private String cookieName = "apimlAuthenticationToken";
        private boolean cookieSecure = true;
        private String cookiePath = "/";
        private String cookieComment = "API Mediation Layer security token";
        private Integer cookieMaxAge = -1;
    }

    @Data
    public static class PassTicket {
        private Integer timeout = 540;
    }

    public AuthConfigurationProperties() {
        this.cookieProperties = new AuthConfigurationProperties.CookieProperties();
        this.tokenProperties = new AuthConfigurationProperties.TokenProperties();
        this.passTicket = new AuthConfigurationProperties.PassTicket();
    }

    /**
     * Return the z/OSMF service id when it is set
     *
     * @return the z/OSMF service id
     * @throws AuthenticationServiceException if the z/OSMF service id is not configured
     */
    public String validatedZosmfServiceId() {
        if ((zosmfServiceId == null) || zosmfServiceId.isEmpty()) {
            apimlLog.log("org.zowe.apiml.security.zosmfNotFound");
            throw new AuthenticationServiceException("The parameter 'zosmfServiceId' is not configured.");
        }

        return zosmfServiceId;
    }
}
