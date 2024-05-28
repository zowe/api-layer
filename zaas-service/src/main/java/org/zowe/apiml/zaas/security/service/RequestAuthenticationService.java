/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * Provide syntax sugar for {@link AuthenticationService}
 */
@RequiredArgsConstructor
@Slf4j
public class RequestAuthenticationService {
    private final AuthenticationService authenticationService;

    /**
     * Return information about user retrieved from the request.
     *
     * @param request Request to analyse
     * @return Either username of the current user or nothing.
     */
    public Optional<String> getAuthenticatedUser(HttpServletRequest request) {
        String jwtToken = authenticationService.getJwtTokenFromRequest(request).orElse(null);
        if (jwtToken != null) {
            TokenAuthentication authentication = authenticationService.validateJwtToken(jwtToken);
            if (authentication.isAuthenticated()) {
                return Optional.of(authentication.getPrincipal());
            }
        }

        return Optional.empty();
    }

    /**
     * Extracts principal from JWT token in request without validating it
     * This method is designed to not fail but rather return empty
     *
     * @param request
     * @return Optional containing the username
     */
    public Optional<String> getPrincipalFromRequest(HttpServletRequest request) {
        String jwtToken = authenticationService.getJwtTokenFromRequest(request).orElse(null);
        if (jwtToken == null) {
            return Optional.empty();
        } else {
            try {
                QueryResponse queryResponse = authenticationService.parseJwtToken(jwtToken);
                return Optional.of(queryResponse.getUserId());
            } catch (AuthenticationException e) {
                log.debug("Exception getting principal from request, returning empty principal.", e);
                return Optional.empty();
            }
        }
    }
}
