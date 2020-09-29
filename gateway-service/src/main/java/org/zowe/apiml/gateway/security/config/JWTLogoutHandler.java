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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.security.common.error.AuthenticationTokenException;
import org.zowe.apiml.security.common.handler.FailedAuthenticationHandler;
import org.zowe.apiml.security.common.token.TokenFormatNotValidException;
import org.zowe.apiml.security.common.token.TokenNotProvidedException;
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
            if (token.isPresent()) {
                invalidateJwtToken(failure, request, response, token.get());
            } else {
                failure.onAuthenticationFailure(request, response, new TokenNotProvidedException("The token you are trying to logout is not present in the header"));
            }
        } catch (ServletException e) {
            log.error("The response cannot be written during the logout exception handler: {}", e.getMessage());
        }
    }

    private void invalidateJwtToken(FailedAuthenticationHandler failure, HttpServletRequest request, HttpServletResponse response, String token) throws ServletException {
        if (authenticationService.isInvalidated(token)) {
            failure.onAuthenticationFailure(request, response, new TokenNotValidException("The token you are trying to logout is not valid"));
        } else {
            try {
                authenticationService.invalidateJwtToken(token, true);
            } catch (TokenNotValidException e) {
                failure.onAuthenticationFailure(request, response, new TokenFormatNotValidException(e.getMessage()));
            } catch (AuthenticationException e) {
                failure.onAuthenticationFailure(request, response, e);
            } catch (Exception e) {
                failure.onAuthenticationFailure(request, response, new AuthenticationTokenException("Error while logging out token"));
            }
        }
    }
}
