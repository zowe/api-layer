/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;

import java.util.Map;

/**
 * Common interface for z/OSMF authentication operations. This interface is implemented with each bean for
 * authentication to z/OSMF and also in {@link org.zowe.apiml.gateway.security.service.zosmf.ZosmfServiceFacade}, which
 * provides calls by version of z/OSMF.
 */
public interface ZosmfService {

    /**
     * Make authentication in z/OSMF. The result contains all supported tokens (JWT, LTPA) if they are available and
     * domain (required to construct Zowe's JWT token)
     *
     * @param authentication user authentication
     * @return AuthenticationResponse, collections of supported tokens and domain
     */
    AuthenticationResponse authenticate(Authentication authentication);

    /**
     * Check whether the token is valid in z/OSMF. If token is invalid or any other error occurs it
     * throws an exception.
     *
     * @param type  Type of token (JWT, LTPA)
     * @param token Token to verify
     */
    void validate(ZosmfService.TokenType type, String token);

    /**
     * This method invalidates token in z/OSMF when deactivate functionality is available
     *
     * @param type  Type of token (JWT, LTPA)
     * @param token Token to verify
     */
    void invalidate(ZosmfService.TokenType type, String token);

    /**
     * Method is to decide which version of z/OSMF are supported by implementation.
     * If bean is not real implementation but delegates it has to return false (see
     * {@link org.zowe.apiml.gateway.security.service.zosmf.ZosmfServiceFacade}).
     *
     * @param version version of z/OSMF
     * @return if bean provides implementation for specific version and
     *         configuration of z/OSMF
     */
    boolean isSupported(int version);

    /**
     * Enumeration of supported security tokens
     */
    @AllArgsConstructor
    @Getter
    enum TokenType {

        JWT("jwtToken"),
        LTPA("LtpaToken2");

        private final String cookieName;

    }

    /**
     * Response of authentication, contains all data to next processing
     */
    @Data
    @AllArgsConstructor
    @RequiredArgsConstructor
    class AuthenticationResponse {

        private String domain;
        private final Map<TokenType, String> tokens;

    }

}
