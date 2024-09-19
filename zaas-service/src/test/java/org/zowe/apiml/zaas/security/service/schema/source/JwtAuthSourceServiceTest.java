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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.token.QueryResponse.Source;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource.Origin;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource.Parsed;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthSourceServiceTest {
    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private JwtAuthSourceService serviceUnderTest;

    private final String token = "jwtToken";
    private JwtAuthSource jwtAuthSource;
    private TokenAuthentication tokenAuthentication;
    private Parsed expectedParsedSource;

    @BeforeEach
    public void setup() {
        jwtAuthSource = new JwtAuthSource("jwtToken");
        tokenAuthentication = TokenAuthentication.createAuthenticated("user", token);
        expectedParsedSource = new ParsedTokenAuthSource("user", new Date(111), new Date(222), Origin.ZOSMF);
    }

    @Test
    void givenZosmfTokenInRequest_thenAuthSourceIsPresent() {
        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of(token));
        when(authenticationService.getTokenOrigin(token)).thenReturn(Origin.ZOSMF);

        Optional<AuthSource> authSource = serviceUnderTest.getAuthSourceFromRequest(request);

        verify(authenticationService, times(1)).getJwtTokenFromRequest(request);
        Assertions.assertTrue(authSource.isPresent());
        Assertions.assertTrue(authSource.get() instanceof JwtAuthSource);
        Assertions.assertEquals(token, authSource.get().getRawSource());
    }

    @Test
    void givenZoweTokenInRequest_thenAuthSourceIsPresent() {
        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of(token));
        when(authenticationService.getTokenOrigin(token)).thenReturn(Origin.ZOWE);

        Optional<AuthSource> authSource = serviceUnderTest.getAuthSourceFromRequest(request);

        verify(authenticationService, times(1)).getJwtTokenFromRequest(request);
        Assertions.assertTrue(authSource.isPresent());
        Assertions.assertTrue(authSource.get() instanceof JwtAuthSource);
        Assertions.assertEquals(token, authSource.get().getRawSource());
    }

    @Test
    void givenPatTokenInRequest_thenAuthSourceIsNotPresent() {
        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of(token));
        when(authenticationService.getTokenOrigin(token)).thenReturn(Origin.ZOWE_PAT);

        Optional<AuthSource> authSource = serviceUnderTest.getAuthSourceFromRequest(request);

        verify(authenticationService, times(1)).getJwtTokenFromRequest(request);
        Assertions.assertFalse(authSource.isPresent());
    }

    @Test
    void givenNoTokenInRequest_thenAuthSourceIsPresent() {
        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.empty());

        Optional<AuthSource> authSource = serviceUnderTest.getAuthSourceFromRequest(request);

        verify(authenticationService, times(1)).getJwtTokenFromRequest(request);
        Assertions.assertFalse(authSource.isPresent());
    }

    @Test
    void givenInvalidAuthSource_thenAuthSourceIsInvalid() {
        TokenAuthentication tokenAuth = new TokenAuthentication("user");
        tokenAuth.setAuthenticated(false);
        when(authenticationService.validateJwtToken(anyString())).thenReturn(tokenAuth);

        Assertions.assertFalse(serviceUnderTest.isValid(jwtAuthSource));
        verify(authenticationService, times(1)).validateJwtToken(token);
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class GivenInvalidAuthSource {
        @Nested
        class WhenNullAuthSource {
            @Test
            void thenAuthSourceIsInvalid() {
                Assertions.assertFalse(serviceUnderTest.isValid(null));
                verifyNoInteractions(authenticationService);
            }

            @Test
            void thenParsedIsNull() {
                Assertions.assertNull(serviceUnderTest.parse(null));
                verifyNoInteractions(authenticationService);
            }

            @Test
            void thenNullLtpa() {
                Assertions.assertNull(serviceUnderTest.getLtpaToken(null));
                verifyNoInteractions(authenticationService);
            }
        }

        @Nested
        class WhenNullTokenInAuthSource {
            private final AuthSource authSourceNullToken = new JwtAuthSource(null);

            @Test
            void givenNullTokenInAuthSource_thenAuthSourceIsInvalid() {
                Assertions.assertFalse(serviceUnderTest.isValid(authSourceNullToken));
                verifyNoInteractions(authenticationService);
            }

            @Test
            void givenNullTokenInAuthSource_thenParsedIsNull() {
                Assertions.assertNull(serviceUnderTest.parse(authSourceNullToken));
                verifyNoInteractions(authenticationService);
            }

            @Test
            void givenNullTokenInAuthSource_thenNullLtpa() {
                Assertions.assertNull(serviceUnderTest.getLtpaToken(authSourceNullToken));
                verifyNoInteractions(authenticationService);
            }
        }
    }

    @Nested
    class GivenUnknownAuthSource {
        private final AuthSource dummyAuthSource = new DummyAuthSource();

        @Test
        void whenParse_thenNull() {
            Assertions.assertNull(serviceUnderTest.parse(dummyAuthSource));
            verifyNoInteractions(authenticationService);
        }

        @Test
        void thenIsInvalid() {
            Assertions.assertFalse(serviceUnderTest.isValid(dummyAuthSource));
            verifyNoInteractions(authenticationService);
        }

        @Test
        void whenGetLtpa_thenNull() {
            Assertions.assertNull(serviceUnderTest.getLtpaToken(dummyAuthSource));
            verifyNoInteractions(authenticationService);
        }
    }

    @Nested
    class GivenValidAuthSource {
        @Test
        void thenIsValid() {
            when(authenticationService.validateJwtToken(anyString())).thenReturn(tokenAuthentication);

            Assertions.assertTrue(serviceUnderTest.isValid(jwtAuthSource));
            verify(authenticationService, times(1)).validateJwtToken(token);
        }

        @Test
        void thenParseCorrectly() {
            when(authenticationService.parseJwtToken(anyString())).thenReturn(new QueryResponse("domain", "user", new Date(111), new Date(222), "issuer", Collections.emptyList(), Source.ZOSMF));

            Parsed parsedSource = serviceUnderTest.parse(jwtAuthSource);

            verify(authenticationService, times(1)).parseJwtToken(token);
            Assertions.assertNotNull(parsedSource);
            Assertions.assertEquals(expectedParsedSource, parsedSource);
        }

        @Test
        void thenLtpaGenerated() {
            String ltpa = "ltpaToken";
            when(authenticationService.getLtpaTokenWithValidation(anyString())).thenReturn(ltpa);

            Assertions.assertEquals(ltpa, serviceUnderTest.getLtpaToken(jwtAuthSource));
            verify(authenticationService, times(1)).getLtpaTokenWithValidation(token);
        }
    }

    @Nested
    class GivenTokenNotValidException {
        private final TokenNotValidException exception = new TokenNotValidException("token not valid");

        @Test
        void whenIsValid_thenThrow() {
            when(authenticationService.validateJwtToken(anyString())).thenThrow(exception);

            assertThrows(TokenNotValidException.class, () -> serviceUnderTest.isValid(jwtAuthSource));
            verify(authenticationService, times(1)).validateJwtToken(token);
        }

        @Test
        void whenParse_thenThrow() {
            when(authenticationService.parseJwtToken(anyString())).thenThrow(exception);

            assertThrows(TokenNotValidException.class, () -> serviceUnderTest.parse(jwtAuthSource));
            verify(authenticationService, times(1)).parseJwtToken(token);
        }

        @Test
        void whenGetLtpa_thenThrow() {
            when(authenticationService.getLtpaTokenWithValidation(anyString())).thenThrow(exception);

            assertThrows(TokenNotValidException.class, () -> serviceUnderTest.getLtpaToken(jwtAuthSource));
            verify(authenticationService, times(1)).getLtpaTokenWithValidation(token);
        }

        @Test
        void whenGetToken_thenThrow() {
            when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of(token));
            when(authenticationService.getTokenOrigin(token)).thenThrow(exception);

            assertThrows(TokenNotValidException.class, () -> serviceUnderTest.getToken(request));
            verify(authenticationService, times(1)).getTokenOrigin(token);
        }
    }

    @Nested
    class GivenTokenExpireException {
        private final TokenExpireException exception = new TokenExpireException("token expired");

        @Test
        void whenIsValid_thenThrow() {
            when(authenticationService.validateJwtToken(anyString())).thenThrow(exception);

            assertThrows(TokenExpireException.class, () -> serviceUnderTest.isValid(jwtAuthSource));
            verify(authenticationService, times(1)).validateJwtToken("jwtToken");
        }

        @Test
        void whenParse_thenThrow() {
            when(authenticationService.parseJwtToken(anyString())).thenThrow(exception);

            assertThrows(TokenExpireException.class, () -> serviceUnderTest.parse(jwtAuthSource));
            verify(authenticationService, times(1)).parseJwtToken(token);
        }

        @Test
        void whenGetLtpa_thenThrow() {
            when(authenticationService.getLtpaTokenWithValidation(anyString())).thenThrow(exception);

            assertThrows(TokenExpireException.class, () -> serviceUnderTest.getLtpaToken(jwtAuthSource));
            verify(authenticationService, times(1)).getLtpaTokenWithValidation(token);
        }

        @Test
        void whenGetToken_thenThrow() {
            when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of(token));
            when(authenticationService.getTokenOrigin(token)).thenThrow(exception);

            assertThrows(TokenExpireException.class, () -> serviceUnderTest.getToken(request));
            verify(authenticationService, times(1)).getTokenOrigin(token);
        }
    }

    static class DummyAuthSource implements AuthSource {
        @Override
        public Object getRawSource() {
            return null;
        }

        @Override
        public AuthSourceType getType() {
            return null;
        }
    }
}
