/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.token;

import lombok.EqualsAndHashCode;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Collections;

/**
 * This object is added to security context after successful authentication.
 * Contains username nad valid JWT token.
 */
@EqualsAndHashCode(callSuper = false)
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
}
