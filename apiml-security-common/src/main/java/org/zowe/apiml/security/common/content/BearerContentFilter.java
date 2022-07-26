/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.content;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.security.common.error.ResourceAccessExceptionHandler;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * Authenticate the JWT token stored in Bearer header
 */
public class BearerContentFilter extends AbstractSecureContentFilter {

    public BearerContentFilter(AuthenticationManager authenticationManager,
                               AuthenticationFailureHandler failureHandler,
                               ResourceAccessExceptionHandler resourceAccessExceptionHandler) {
        super(authenticationManager, failureHandler, resourceAccessExceptionHandler, new String[0]);
    }

    public BearerContentFilter(AuthenticationManager authenticationManager,
                               AuthenticationFailureHandler failureHandler,
                               ResourceAccessExceptionHandler resourceAccessExceptionHandler,
                               String[] endpoints) {
        super(authenticationManager, failureHandler, resourceAccessExceptionHandler, endpoints);
    }

    /**
     * Extract the JWT token from the authorization header
     *
     * @param request the http request
     * @return the JWT token
     */
    protected Optional<AbstractAuthenticationToken> extractContent(HttpServletRequest request) {
        return Optional.ofNullable(
            request.getHeader(HttpHeaders.AUTHORIZATION)
        ).filter(
            header -> header.startsWith(ApimlConstants.BEARER_AUTHENTICATION_PREFIX)
        ).map(
            header -> {
                header = header.replaceFirst(ApimlConstants.BEARER_AUTHENTICATION_PREFIX, "").trim();
                return new TokenAuthentication(header);
            }
        );
    }
}
