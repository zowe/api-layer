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

import lombok.RequiredArgsConstructor;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * Provide syntax sugar for HTTP based authentication
 */
@RequiredArgsConstructor
public class HttpAuthenticationService {
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
}
