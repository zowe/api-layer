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

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.zowe.apiml.security.common.error.ResourceAccessExceptionHandler;
import org.zowe.apiml.security.common.handler.FailedAuthenticationHandler;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import java.util.Optional;

import static org.zowe.apiml.constants.ApimlConstants.HEADER_OIDC_TOKEN;
import static org.zowe.apiml.security.common.token.TokenAuthentication.Type.OIDC;

public class OidcContentFilter extends AbstractSecureContentFilter {

    public OidcContentFilter(AuthenticationManager authenticationManager, FailedAuthenticationHandler authenticationFailureHandler, ResourceAccessExceptionHandler resourceAccessExceptionHandler) {
        super(authenticationManager, authenticationFailureHandler, resourceAccessExceptionHandler, new String[0]);
    }

    @Override
    protected Optional<AbstractAuthenticationToken> extractContent(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(HEADER_OIDC_TOKEN))
            .map(token -> new TokenAuthentication(token, OIDC));
    }

}
