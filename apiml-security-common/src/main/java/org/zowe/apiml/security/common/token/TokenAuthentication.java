/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.token;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.zowe.apiml.security.common.login.LoginFilter;

import java.util.Collections;
import java.util.Optional;

/**
 * This object is added to security context after successful authentication.
 * Contains username and valid JWT token.
 */
@EqualsAndHashCode(callSuper = false)
public class TokenAuthentication extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 9187160928171618141L;

    private final String username;
    private final String token;
    @Getter
    private Type type;

    public TokenAuthentication(String token) {
        this(token, (Type) null);
    }

    public TokenAuthentication(String token, Type type) {
        this(null, token, type);
    }

    public TokenAuthentication(String username, String token) {
        this(username, token, (Type) null);
    }

    public TokenAuthentication(String username, String token, Type type) {
        super(Collections.emptyList());
        this.username = username;
        this.token = token;
        this.type = type;
    }

    /**
     * @return the token that prove the username is correct
     */
    @Override
    public String getCredentials() {
        return token;
    }

    /**
     * @return the username being authenticated
     */
    @Override
    public String getPrincipal() {
        return username;
    }

    /**
     * Creates the TokenAuthentication with fulfilled username (principal), token and marked as authenticated.
     * @param username Username, who is authenticated
     * @param token Token, which authenticate the user
     * @return TokenAuthentication marked as authenticated with username, token
     */
    public static TokenAuthentication createAuthenticated(String username, String token, Type type) {
        final TokenAuthentication out = new TokenAuthentication(username, token, type);
        out.setAuthenticated(true);
        return out;
    }

    @SuppressWarnings("squid:S3655")
    public static TokenAuthentication createAuthenticatedFromHeader(String token, String authHeader) {
        var loginRequest = LoginFilter.getCredentialFromAuthorizationHeader(Optional.of(authHeader));
        return createAuthenticated(loginRequest.get().getUsername(), token, Type.JWT);
    }

    public enum Type {
        JWT,
        OIDC
    }

}
