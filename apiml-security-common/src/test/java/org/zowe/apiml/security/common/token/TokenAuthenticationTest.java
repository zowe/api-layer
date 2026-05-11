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

import static org.junit.jupiter.api.Assertions.*;
import static org.zowe.apiml.security.common.token.TokenAuthentication.Type.JWT;
import static org.zowe.apiml.security.common.token.TokenAuthentication.Type.OIDC;

class TokenAuthenticationTest {

    public static final String USERNAME = "user";
    public static final String JWT_TOKEN = JWTTestUtils.createDummyAPIMLToken(USERNAME);
    public static final String EXPIRED_JWT_TOKEN = JWTTestUtils.createDummyJwtToken(USERNAME, "APIML", -100_000L);

    @Test
    void testCreateAuthenticated() {
        var ta = TokenAuthentication.createAuthenticated(USERNAME, JWT_TOKEN, JWT);
        validateTokenAuthentication(ta, JWT_TOKEN, true);
    }

    @Test
    void testCreateAuthenticatedFromHeader() {
        var ta = TokenAuthentication.createAuthenticatedFromHeader(JWT_TOKEN, "Basic dXNlcjpwYXNzd29yZA==");
        validateTokenAuthentication(ta, JWT_TOKEN, true);
    }

    @Test
    void testAuthenticationFalseByDefault() {
        var ta = new TokenAuthentication(JWT_TOKEN);
        validateTokenAuthentication(ta, JWT_TOKEN, false);
    }

    @Test
    void testExceptionThrownOnUnparsableToken() {
        assertThrows(TokenNotValidException.class, () -> new TokenAuthentication("unparsableToken"));
    }

    @Test
    void testExpiredTokenDoesNotFailParsing() {
        var ta = new TokenAuthentication(EXPIRED_JWT_TOKEN);
        validateTokenAuthentication(ta, EXPIRED_JWT_TOKEN, false);
    }

    @Test
    void testSettingAuthenticationTrueWithExpiredTokenFails() {
        var ta = new TokenAuthentication(EXPIRED_JWT_TOKEN);
        assertThrows(TokenExpireException.class, () -> ta.setAuthenticated(true));
    }

    @Test
    void testSettingAuthenticationFalseWithExpiredTokenSucceeds() {
        var ta = new TokenAuthentication(EXPIRED_JWT_TOKEN);
        ta.setAuthenticated(false);

        validateTokenAuthentication(ta, EXPIRED_JWT_TOKEN, false);
    }

    @Test
    void testOIDCTokenIsParsed() {
        var user = "distributed-id";
        var provider = "https://oidc-provider";
        var oidcToken = JWTTestUtils.createDummyJwtToken(user, provider);
        var ta = new TokenAuthentication(user, oidcToken, OIDC);

        assertEquals(user, ta.getPrincipal());
        assertEquals(oidcToken, ta.getCredentials());
        assertEquals(OIDC, ta.getType());
        assertEquals(QueryResponse.Source.OIDC, ta.getSource());
        assertEquals(provider, ta.getQueryResponse().getIssuer());
        assertFalse(ta.isAuthenticated());
    }

    private void validateTokenAuthentication(TokenAuthentication ta, String jwt, boolean isAuthenticated) {
        assertEquals(USERNAME, ta.getPrincipal());
        assertEquals(jwt, ta.getCredentials());
        assertEquals(JWT, ta.getType());
        assertEquals(QueryResponse.Source.ZOWE, ta.getSource());
        assertEquals("APIML", ta.getQueryResponse().getIssuer());
        assertEquals(isAuthenticated, ta.isAuthenticated());
    }

}
