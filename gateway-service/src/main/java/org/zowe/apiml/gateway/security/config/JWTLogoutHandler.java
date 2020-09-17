/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.config;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.security.common.handler.FailedAuthenticationHandler;
import org.zowe.apiml.security.common.token.TokenFormatNotValidException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

@Slf4j
@AllArgsConstructor
public class JWTLogoutHandler implements LogoutHandler {

    private final AuthenticationService authenticationService;
    private final FailedAuthenticationHandler failure;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        Optional<String> token = authenticationService.getJwtTokenFromRequest(request);
        try {
            checkJwtTokenFormat(failure, request, response, token);
        } catch (ServletException e) {
            log.error("The response cannot be written during the logout exception handler: {}", e.getMessage());
        }
    }

    private void checkJwtTokenFormat(FailedAuthenticationHandler failure, HttpServletRequest request, HttpServletResponse response, Optional<String> token) throws ServletException {
        if (token.isPresent()) {
            try {
                authenticationService.invalidateJwtToken(token.get(), true);
            } catch (TokenNotValidException e) {
                failure.onAuthenticationFailure(request, response, new TokenFormatNotValidException(e.getMessage()));
            }
        } else {
            failure.onAuthenticationFailure(request, response, new TokenFormatNotValidException("The token you are trying to logout is not valid or not present in the header"));
        }
    }
}
