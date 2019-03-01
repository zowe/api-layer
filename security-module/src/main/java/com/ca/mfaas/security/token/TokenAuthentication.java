/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.security.token;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Collections;

/**
 * This object is added to security context after successful authentication.
 * Contains username nad valid JWT token.
 */
public class TokenAuthentication extends AbstractAuthenticationToken {
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

    @Override
    public String getCredentials() {
        return token;
    }

    @Override
    public String getPrincipal() {
        return username;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof TokenAuthentication)) return false;
        final TokenAuthentication other = (TokenAuthentication) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$username = this.username;
        final Object other$username = other.username;
        if (this$username == null ? other$username != null : !this$username.equals(other$username)) return false;
        final Object this$token = this.token;
        final Object other$token = other.token;
        if (this$token == null ? other$token != null : !this$token.equals(other$token)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof TokenAuthentication;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $username = this.username;
        result = result * PRIME + ($username == null ? 43 : $username.hashCode());
        final Object $token = this.token;
        result = result * PRIME + ($token == null ? 43 : $token.hashCode());
        return result;
    }
}
