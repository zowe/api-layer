/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;


class JwtUtilsTest {

    @Test
    void testHandleJwtParserExceptionForExpiredToken() {

        Exception exception = JwtUtils.handleJwtParserException(new ExpiredJwtException(mock(Header.class), mock(Claims.class), "msg"));
        assertTrue(exception instanceof TokenExpireException);
        assertEquals("Token is expired.", exception.getMessage());
    }

    @Test
    void testHandleJwtParserExceptionForInvalidToken() {

        Exception exception = JwtUtils.handleJwtParserException(new JwtException("msg"));
        assertTrue(exception instanceof TokenNotValidException);
        assertEquals("Token is not valid.", exception.getMessage());
    }

    @Test
    void testHandleJwtParserRuntimeException() {
        Exception exception = JwtUtils.handleJwtParserException(new RuntimeException("msg"));
        assertTrue(exception instanceof TokenNotValidException);
        assertEquals("An internal error occurred while validating the token therefore the token is no longer valid.", exception.getMessage());
    }
}
