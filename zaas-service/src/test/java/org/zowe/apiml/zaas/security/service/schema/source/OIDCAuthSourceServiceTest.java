/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.schema.source;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.zowe.apiml.security.common.token.NoMainframeIdentityException;
import org.zowe.apiml.security.common.token.OIDCProvider;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.zaas.security.mapping.AuthenticationMapper;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import org.zowe.apiml.zaas.security.service.TokenCreationService;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OIDCAuthSourceServiceTest {


    private TokenCreationService tokenCreationService;
    private OIDCAuthSourceService service;
    private AuthenticationService authenticationService;
    private OIDCProvider provider;
    private AuthenticationMapper mapper;
    private static final String DUMMY_TOKEN = "token";
    private static final String TOKEN_WITH_USERNAME_FIELDS = "ewogICJ0eXAiOiAiSldUIiwKICAibm9uY2UiOiAiYVZhbHVlVG9CZVZlcmlmaWVkIiwKICAiYWxnIjogIlJTMjU2IiwKICAia2lkIjogIlNlQ1JldEtleSIKfQ.ewogICJhdWQiOiAiMDAwMDAwMDMtMDAwMC0wMDAwLWMwMDAtMDAwMDAwMDAwMDAwIiwKICAiaXNzIjogImh0dHBzOi8vb2lkYy5wcm92aWRlci5vcmcvYXBwIiwKICAiaWF0IjogMTcyMjUxNDEyOSwKICAibmJmIjogMTcyMjUxNDEyOSwKICAiZXhwIjogODcyMjUxODEyNSwKICAic3ViIjogIm9pZGMudXNlcm5hbWUiLAogICJlbWFpbCI6ICJ1c2VybmFtZUBvaWRjLm9yZyIsCiAgIm9yZyI6IHsKICAgICJuYW1lIjogIm9wZW5tYWluZnJhbWUiLAogICAgImRlcCI6IHsKICAgICAgIm5hbWUiOiAiem93ZSIsCiAgICAgICJ0ZWFtIjogImFwaW1sIiwKICAgICAgImNvbnRyaWJ1dG9yIjogImNvbnRyaWJ1dG9yQGFwaW1sLnpvd2UiLAogICAgICAibmlja25hbWUiOiAiIiwKICAgICAgIm51bGxWYWx1ZSI6IG51bGwKICAgIH0KICB9LAogICJtZW1iZXJPZiI6IFsKICAgICJvcGVubWFpbmZyYW1lIiwKICAgICJ6b3dlIiwKICAgICJhcGltbCIKICBdCn0.c29tZVNpZ25lZEhhc2hDb2Rl";
    public static final String SUB_USER = "oidc.username";
    private static final String MF_USER = "MF_USER";
    private static final String DEFAULT_USERID_FIELD = "sub";

    @BeforeEach
    void setup() {
        authenticationService = mock(AuthenticationService.class);
        tokenCreationService = mock(TokenCreationService.class);
        provider = mock(OIDCProvider.class);
        mapper = mock(AuthenticationMapper.class);
        service = new OIDCAuthSourceService(mapper, authenticationService, provider, tokenCreationService);
        service.userIdFieldPathProperty = DEFAULT_USERID_FIELD;
        service.afterPropertiesSet();
    }

    @Test
    void returnOIDCSourceMapper() {
        assertTrue(service.getMapper().apply(DUMMY_TOKEN) instanceof OIDCAuthSource);
    }

    @Test
    void returnLogger() {
        assertNotNull(service.getLogger());
    }

    @Nested
    class GivenValidTokenTest {
        @Test
        void givenOidcTokenInRequestContext_thenReturnTheToken() {
            HttpServletRequest request = new MockHttpServletRequest();
            when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of(DUMMY_TOKEN));
            when(authenticationService.getTokenOrigin(DUMMY_TOKEN)).thenReturn(AuthSource.Origin.OIDC);
            Optional<String> tokenResult = service.getToken(request);
            assertTrue(tokenResult.isPresent());
            assertEquals(DUMMY_TOKEN, tokenResult.get());
        }

        @Test
        void givenPatTokenInRequestContext_thenReturnEmpty() {
            HttpServletRequest request = new MockHttpServletRequest();
            when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of(DUMMY_TOKEN));
            when(authenticationService.getTokenOrigin(DUMMY_TOKEN)).thenReturn(AuthSource.Origin.ZOWE_PAT);
            Optional<String> tokenResult = service.getToken(request);
            assertFalse(tokenResult.isPresent());
        }

        @Test
        void givenTokenInAuthSource_thenReturnValid() {
            when(provider.isValid(TOKEN_WITH_USERNAME_FIELDS)).thenReturn(true);
            OIDCAuthSource authSource = new OIDCAuthSource(TOKEN_WITH_USERNAME_FIELDS);

            assertTrue(service.isValid(authSource));
            assertEquals(SUB_USER, authSource.getDistributedId());
        }

        @Test
        void whenParse_thenCorrect() {
            OIDCAuthSource authSource = mockValidAuthSource();
            when(mapper.mapToMainframeUserId(authSource)).thenReturn(MF_USER);
            AuthSource.Parsed parsedSource = service.parse(authSource);

            verify(mapper, times(1)).mapToMainframeUserId(authSource);
            assertEquals(SUB_USER, authSource.getDistributedId());
            assertEquals(MF_USER, parsedSource.getUserId());
        }

        @Test
        void givenNoMapping_whenParse_thenThrowException() {
            when(provider.isValid(TOKEN_WITH_USERNAME_FIELDS)).thenReturn(true);
            OIDCAuthSource authSource = new OIDCAuthSource(TOKEN_WITH_USERNAME_FIELDS);

            assertThrows(NoMainframeIdentityException.class, () -> {
                service.parse(authSource);
            });
        }

        @Test
        void givenValidAuthSource_thenReturnLTPAToken() {
            OIDCAuthSource authSource = mockValidAuthSource();
            String expectedToken = "ltpa-token";
            when(mapper.mapToMainframeUserId(any())).thenReturn(MF_USER);
            String zoweToken = "zowe-token";
            when(tokenCreationService.createJwtTokenWithoutCredentials(MF_USER)).thenReturn(zoweToken);
            when(authenticationService.getTokenOrigin(zoweToken)).thenReturn(AuthSource.Origin.ZOWE);
            when(authenticationService.getLtpaToken(zoweToken)).thenReturn(expectedToken);

            String ltpaResult = service.getLtpaToken(authSource);
            assertEquals(expectedToken, ltpaResult);
            assertEquals(SUB_USER, authSource.getDistributedId());
        }

        @Test
        void givenValidAuthSource_thenReturnJWT() {
            OIDCAuthSource authSource = mockValidAuthSource();
            when(mapper.mapToMainframeUserId(any())).thenReturn(MF_USER);
            String expectedToken = "jwt-token";
            when(tokenCreationService.createJwtTokenWithoutCredentials(MF_USER)).thenReturn(expectedToken);
            String jwtResult = service.getJWT(authSource);
            assertEquals(expectedToken, jwtResult);
            assertEquals(SUB_USER, authSource.getDistributedId());
        }

        @ParameterizedTest
        @CsvSource({
            "email,username@oidc.org",
            "org.dep.contributor, contributor@apiml.zowe"})
        void givenUserIdFieldProperty_thenReturnCorrectUsername(String preferredUsernameField, String username) {
            when(provider.isValid(TOKEN_WITH_USERNAME_FIELDS)).thenReturn(true);
            OIDCAuthSource authSource = new OIDCAuthSource(TOKEN_WITH_USERNAME_FIELDS);

            service.userIdFieldPathProperty = preferredUsernameField;
            service.afterPropertiesSet();

            assertTrue(service.isValid(authSource));
            assertEquals(username, authSource.getDistributedId());
        }

        @ParameterizedTest
        @ValueSource(strings = { "nonexistent", "org.nonexistent.foo", "org.dep", "org.dep.nonexistent", "org.dep.nickname", "org.dep.nullValue"})
        void givenInvalidUserIdFieldProperty_thenThrowException(String preferredUsernameField) {
            when(provider.isValid(TOKEN_WITH_USERNAME_FIELDS)).thenReturn(true);
            OIDCAuthSource authSource = new OIDCAuthSource(TOKEN_WITH_USERNAME_FIELDS);

            service.userIdFieldPathProperty = preferredUsernameField;
            service.afterPropertiesSet();

            assertFalse(service.isValid(authSource));
            assertNull(authSource.getDistributedId());
        }
    }

    @Nested
    class GivenDifferentAuthSourcesTest {

        @Test
        void givenJWTAuthSourceWhenValidating_thenReturnFalse() {
            JwtAuthSource authSource = new JwtAuthSource(DUMMY_TOKEN);
            boolean isValid = service.isValid(authSource);
            assertFalse(isValid);
        }

        @Test
        void givenJWTAuthSource_thenReturnNull() {
            JwtAuthSource authSource = new JwtAuthSource(DUMMY_TOKEN);
            AuthSource.Parsed parsedSource = service.parse(authSource);
            assertNull(parsedSource);
        }
    }

    @Nested
    class GivenInvalidTokenTest {
        @Test
        void whenTokenIsNull_thenReturnTokenInvalid() {
            OIDCAuthSource authSource = new OIDCAuthSource(null);
            assertFalse(service.isValid(authSource));
        }

        @Test
        void whenTokenIsEmpty_thenReturnTokenInvalid() {
            OIDCAuthSource authSource = new OIDCAuthSource("");
            assertFalse(service.isValid(authSource));
        }

        @Test
        void whenIsInvalid_thenReturnTokenInvalid() {
            OIDCAuthSource authSource = new OIDCAuthSource(DUMMY_TOKEN);
            when(provider.isValid(DUMMY_TOKEN)).thenReturn(false);
            assertFalse(service.isValid(authSource));
        }

        @Test
        void whenParse_thenReturnNull() {
            OIDCAuthSource authSource = new OIDCAuthSource(DUMMY_TOKEN);
            when(provider.isValid(DUMMY_TOKEN)).thenReturn(false);
            assertThrows(TokenNotValidException.class, () -> service.parse(authSource));

            verify(mapper, times(0)).mapToMainframeUserId(authSource);
        }

        @Test
        void whenTokenIsExpired_thenThrow() {
            HttpServletRequest request = new MockHttpServletRequest();
            when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of(DUMMY_TOKEN));
            when(authenticationService.getTokenOrigin(DUMMY_TOKEN)).thenThrow(new TokenExpireException("token expired"));

            assertThrows(TokenExpireException.class, () -> service.getToken(request));
            verify(authenticationService, times(1)).getTokenOrigin(DUMMY_TOKEN);
        }

        @Test
        void whenTokenIsNotValid_thenThrow() {
            HttpServletRequest request = new MockHttpServletRequest();
            when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of(DUMMY_TOKEN));
            when(authenticationService.getTokenOrigin(DUMMY_TOKEN)).thenThrow(new TokenNotValidException("token not valid"));

            assertThrows(TokenNotValidException.class, () -> service.getToken(request));
            verify(authenticationService, times(1)).getTokenOrigin(DUMMY_TOKEN);
        }
    }

    private OIDCAuthSource mockValidAuthSource() {
        //No QueryResponse field is validated, so it can have dummy values to simplify mocking.
        QueryResponse tokenResponse = new QueryResponse("domain", "user", new Date(), new Date(), "issuer", Collections.emptyList(), QueryResponse.Source.OIDC);
        when(authenticationService.parseJwtToken(TOKEN_WITH_USERNAME_FIELDS)).thenReturn(tokenResponse);
        when(provider.isValid(TOKEN_WITH_USERNAME_FIELDS)).thenReturn(true);
        return new OIDCAuthSource(TOKEN_WITH_USERNAME_FIELDS);
    }
}
