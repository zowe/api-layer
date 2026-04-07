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

import org.junit.jupiter.api.Test;
import org.zowe.apiml.security.common.util.JWTTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.zowe.apiml.security.common.token.TokenAuthentication.Type.JWT;

class TokenAuthenticationTest {

    public static final String USERNAME = "user";
    public static final String JWT_TOKEN = JWTTestUtils.createDummyAPIMLToken(USERNAME);

    @Test
    void testCreateAuthenticated() {
        TokenAuthentication ta = TokenAuthentication.createAuthenticated(USERNAME, JWT_TOKEN, JWT);
        assertEquals(USERNAME, ta.getPrincipal());
        assertEquals(JWT_TOKEN, ta.getCredentials());
        assertEquals(JWT, ta.getType());
        assertTrue(ta.isAuthenticated());
    }

    @Test
    void testCreateAuthenticatedFromHeader() {
        TokenAuthentication ta = TokenAuthentication.createAuthenticatedFromHeader(JWT_TOKEN, "Basic dXNlcjpwYXNzd29yZA==");
        assertEquals("user", ta.getPrincipal());
        assertEquals(JWT_TOKEN, ta.getCredentials());
        assertTrue(ta.isAuthenticated());
    }

}
