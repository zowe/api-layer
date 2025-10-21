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

import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.ExpiredJWTException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenFormatNotValidException;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.security.common.util.JwtUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.zowe.apiml.zaas.utils.JWTUtils.createTokenWithUserFields;

class JwtUtilsTest {

    private static final String TOKEN_WITH_USERNAME_FIELDS = createTokenWithUserFields();

    @Test
    void testHandleJwtParserExceptionForExpiredToken() {
        Exception exception = JwtUtils.handleJwtParserException(new ExpiredJWTException("msg"));
        assertInstanceOf(TokenExpireException.class, exception);
        assertEquals("Token is expired.", exception.getMessage());
    }

    @Test
    void testHandleJwtParserExceptionForInvalidToken() {
        Exception exception = JwtUtils.handleJwtParserException(new BadJWTException("msg"));
        assertInstanceOf(TokenNotValidException.class, exception);
        assertEquals("Token is not valid.", exception.getMessage());
    }

    @Test
    void testHandleJwtParserRuntimeException() {
        Exception exception = JwtUtils.handleJwtParserException(new RuntimeException("msg"));
        assertInstanceOf(TokenNotValidException.class, exception);
        assertEquals("An internal error occurred while validating the token therefore the token is no longer valid.", exception.getMessage());
    }

    @Test
    void givenJwtNullToken_thenThrowTokenNotValidException() {
        assertThrows(TokenNotValidException.class, () -> JwtUtils.getJwtClaims(null));
    }

    @ParameterizedTest
    @CsvSource({
        "email,username@oidc.org",
        "org.dep.contributor, contributor@apiml.zowe"})
    void givenValidFieldPath_thenReturnCorrectValue(String fieldPath, String expectedValue) {
        var actualValue = JwtUtils.getFieldValuesFromToken(TOKEN_WITH_USERNAME_FIELDS, splitFieldPath(fieldPath));
        assertEquals(List.of(expectedValue), actualValue);
    }

    @ParameterizedTest
    @ValueSource(strings = { "memberOf", "groups.memberOf"})
    void givenValidFieldPath_thenReturnCorrectValuesFromArray(String fieldPath) {
        var actualValue = JwtUtils.getFieldValuesFromToken(TOKEN_WITH_USERNAME_FIELDS, splitFieldPath(fieldPath));
        assertThat(List.of("openmainframe", "zowe", "apiml"), Matchers.containsInAnyOrder(actualValue.toArray()));
    }

    @ParameterizedTest
    @ValueSource(strings = { "nonexistent", "nullValue", "org.nonexistent.foo", "org.dep", "org.dep.nonexistent", "org.dep.nickname"})
    void givenInvalidFieldPath_thenThrowInvalidTokenFormatException(String fieldPath) {
        assertThrows(TokenFormatNotValidException.class, () -> JwtUtils.getFieldValuesFromToken(TOKEN_WITH_USERNAME_FIELDS, splitFieldPath(fieldPath)));
    }

    @Test
    void givenNullToken_thenThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> JwtUtils.getFieldValuesFromToken(null, List.of("foo")));
    }

    @Test
    void givenNullFieldPath_thenThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> JwtUtils.getFieldValuesFromToken(TOKEN_WITH_USERNAME_FIELDS, null));
    }

    @Test
    void givenEmptyFieldPath_thenThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> JwtUtils.getFieldValuesFromToken(TOKEN_WITH_USERNAME_FIELDS, Collections.emptyList()));
    }

    @Test
    void givenEmptyStringFieldPath_thenThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> JwtUtils.getFieldValuesFromToken(TOKEN_WITH_USERNAME_FIELDS, List.of(" ")));
    }


    private List<String> splitFieldPath(String fieldPath) {
        return Arrays.asList(fieldPath.trim().split("\\."));
    }

}
