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
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Collections;

/**
 * This object is added to security context after successful authentication.
 * Contains username and valid JWT token.
 */
@EqualsAndHashCode(callSuper = false)
public class TokenAuthentication extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 9187160928171618141L;

    private final String username;
    private final String token;

    public TokenAuthentication(String token) {
        super(Collections.emptyList());
        this.username = null;
        this.token = token;
    }

    public TokenAuthentication(String username, String token) {
        super(Collections.emptyList());
        this.username = username;
        this.token = token;
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

    public static TokenAuthentication createAuthenticated(String username, String token) {
        final TokenAuthentication out = new TokenAuthentication(username, token);
        out.setAuthenticated(true);
        return out;
    }

}
