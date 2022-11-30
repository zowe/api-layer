/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.login;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.zowe.apiml.security.common.audit.RauditxService;
import org.zowe.apiml.security.common.token.AccessTokenProvider;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

import static org.zowe.apiml.security.common.filter.StoreAccessTokenInfoFilter.TOKEN_REQUEST;

@RequiredArgsConstructor
public class SuccessfulAccessTokenHandler implements AuthenticationSuccessHandler {

    private final AccessTokenProvider accessTokenProvider;
    private final RauditxService rauditxService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        String username = authentication.getName();
        RauditxService.RauditxBuilder rauditBuilder = rauditxService.builder()
            .userId(username)
            .messageSegment("An attempt to generate PAT")
            .alwaysLogSuccesses()
            .alwaysLogFailures();
        try {
            AccessTokenRequest accessTokenRequest = (AccessTokenRequest) request.getAttribute(TOKEN_REQUEST);
            String token = accessTokenProvider.getToken(username, accessTokenRequest.getValidity(), accessTokenRequest.getScopes());
            response.getWriter().print(token);
            response.getWriter().flush();
            response.getWriter().close();
            if (!response.isCommitted()) {
                throw new IOException("Authentication response has not been committed.");
            }
            rauditBuilder.success();
        } catch (RuntimeException | IOException e) {
            rauditBuilder.failure();
            throw e;
        } finally {
            rauditBuilder.issue();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccessTokenRequest {
        private int validity;
        private Set<String> scopes;
    }
}
