/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import org.springframework.security.core.Authentication;

import java.util.Map;

/**
 * Common interface for z/OSMF operations about authentication. This interface is implemented with each bean for
 * authentication to z/OSMF and also in {@link com.ca.mfaas.gateway.security.service.zosmf.ZosmfServiceFacade}, which
 * provides calls by version of z/OSMF.
 */
public interface ZosmfService {

    /**
     * Make authentication in z/OSMF. The result contains all supported token (JWT, LTPA) if they are available and
     * domain (required to construct Zowe's JWT token)
     * @param authentication user authentication
     * @return AuthenticationResponse, collections of supported tokens and domain
     */
    public AuthenticationResponse authenticate(Authentication authentication);

    /**
     * Validate in z/OSMF is the token is valid there or not. If token is invalid or any other error occurred it
     * throws an exception.
     * @param type Type of token (JWT, LTPA)
     * @param token Token to verify
     */
    public void validate(ZosmfService.TokenType type, String token);

    /**
     * This method invalidate token in z/OSMF if the service to deactivate is available
     * @param type Type of token (JWT, LTPA)
     * @param token Token to verify
     */
    public void invalidate(ZosmfService.TokenType type, String token);

    /**
     * Method is to decide which version of z/OSMF are supported by implementation. If bean is not real implementation
     * but delegate it has to return false (see {@link com.ca.mfaas.gateway.security.service.zosmf.ZosmfServiceFacade}).
     * @param version version of z/OSMF
     * @return if bean provides implementation for specific version of z/OSMF
     */
    public boolean matchesVersion(int version);

    /**
     * Enumeration of supported security tokens
     */
    @AllArgsConstructor
    @Getter
    public enum TokenType {

        JWT("jwtToken"),
        LTPA("LtpaToken2")

        ;

        private final String cookieName;

    }

    /**
     * Response of authentication, contains all data to next processing
     */
    @Value
    public static class AuthenticationResponse {

        private final String domain;
        private final Map<TokenType, String> tokens;

    }

}
