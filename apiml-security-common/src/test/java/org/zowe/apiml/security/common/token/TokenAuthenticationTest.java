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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.zowe.apiml.security.common.token.TokenAuthentication.Type.JWT;

class TokenAuthenticationTest {

    @Test
    void testCreateAuthenticated() {
        TokenAuthentication ta = TokenAuthentication.createAuthenticated("user", "token", JWT);
        assertEquals("user", ta.getPrincipal());
        assertEquals("token", ta.getCredentials());
        assertEquals(JWT, ta.getType());
        assertTrue(ta.isAuthenticated());
    }

    @Test
    void testCreateAuthenticatedFromHeader() {
        TokenAuthentication ta = TokenAuthentication.createAuthenticatedFromHeader("user", "Basic dXNlcjpwYXNzd29yZA==");
        assertEquals("user", ta.getPrincipal());
        assertEquals("user", ta.getCredentials());
        assertTrue(ta.isAuthenticated());
    }

}
