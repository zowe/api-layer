/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.zowe.apiml.constants.ApimlConstants;

import javax.servlet.http.Cookie;
import java.util.Optional;

import static org.junit.Assert.*;

public class TokenUtilsTest {
    private static final String COOKIE_NAME = "apimlAuthenticationToken";
    private static final String TOKEN_VALUE = "token";

    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setup() {
        mockRequest = new MockHttpServletRequest();
    }

    @Test
    public void givenJwtInCookie_whenGetToken_returnToken() {
        Cookie[] cookies = new Cookie[]{new Cookie(COOKIE_NAME, TOKEN_VALUE)};
        mockRequest.setCookies(cookies);

        Optional<String> token = TokenUtils.getJwtTokenFromRequest(mockRequest, COOKIE_NAME);
        assertTrue(token.isPresent());
        assertEquals(TOKEN_VALUE, token.get());
    }

    @Test
    public void givenJwtInAuthHeader_whenGetToken_returnToken() {
        mockRequest.addHeader(HttpHeaders.AUTHORIZATION, ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + TOKEN_VALUE);

        Optional<String> token = TokenUtils.getJwtTokenFromRequest(mockRequest, COOKIE_NAME);
        assertTrue(token.isPresent());
        assertEquals(TOKEN_VALUE, token.get());
    }

    @Test
    public void givenNoJwtInAuthHeader_whenGetToken_returnToken() {
        mockRequest.addHeader(HttpHeaders.AUTHORIZATION, ApimlConstants.BEARER_AUTHENTICATION_PREFIX);

        Optional<String> token = TokenUtils.getJwtTokenFromRequest(mockRequest, COOKIE_NAME);
        assertFalse(token.isPresent());
    }

    @Test
    public void givenJwtInInvalidAuthHeader_whenGetToken_returnEmpty() {
        mockRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bad prefix " + TOKEN_VALUE);

        Optional<String> token = TokenUtils.getJwtTokenFromRequest(mockRequest, COOKIE_NAME);
        assertFalse(token.isPresent());
    }

    @Test
    public void givenNoJwt_whenGetToken_returnEmpty() {
        Optional<String> token = TokenUtils.getJwtTokenFromRequest(mockRequest, COOKIE_NAME);
        assertFalse(token.isPresent());
    }
}
