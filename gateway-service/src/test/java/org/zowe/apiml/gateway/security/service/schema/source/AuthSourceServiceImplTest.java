/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service.schema.source;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource.Parsed;
import org.zowe.apiml.gateway.utils.CleanCurrentRequestContextTest;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.token.QueryResponse.Source;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

@ExtendWith(MockitoExtension.class)
class AuthSourceServiceImplTest extends CleanCurrentRequestContextTest {
    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AuthSourceServiceImpl serviceUnderTest;

    @Test
    void givenTokenInRequest_whenGetAuthSource_thenAuthSourceIsPresent() {
        String jwtToken = "jwtToken";
        when(authenticationService.getJwtTokenFromRequest(any())).thenReturn(Optional.of(jwtToken));

        Optional<AuthSource> authSource = serviceUnderTest.getAuthSource();

        verify(authenticationService, times(1)).getJwtTokenFromRequest(any());
        Assertions.assertTrue(authSource.isPresent());
        Assertions.assertTrue(authSource.get() instanceof JwtAuthSource);
        Assertions.assertEquals(jwtToken, authSource.get().getSource());
    }

    @Test
    void givenNoTokenInRequest_whenGetAuthSource_thenAuthSourceIsPresent() {
        when(authenticationService.getJwtTokenFromRequest(any())).thenReturn(Optional.empty());

        Optional<AuthSource> authSource = serviceUnderTest.getAuthSource();

        verify(authenticationService, times(1)).getJwtTokenFromRequest(any());
        Assertions.assertFalse(authSource.isPresent());
    }

    @Test
    void givenNullAuthSource_whenIsValid_thenFalse() {
        Assertions.assertFalse(serviceUnderTest.isValid(null));
        verifyNoInteractions(authenticationService);
    }

    @Test
    void givenNullTokenInAuthSource_whenIsValid_thenFalse() {
        Assertions.assertFalse(serviceUnderTest.isValid(new JwtAuthSource(null)));
        verifyNoInteractions(authenticationService);
    }

    @Test
    void givenUnknownAuthSource_whenIsValid_thenFalse() {
        Assertions.assertFalse(serviceUnderTest.isValid(new DummyAuthSource()));
        verifyNoInteractions(authenticationService);
    }

    @Test
    void givenValidAuthSource_whenIsValid_thenTrue() {
        TokenAuthentication tokenAuthentication = new TokenAuthentication("user");
        tokenAuthentication.setAuthenticated(true);
        when(authenticationService.validateJwtToken(anyString())).thenReturn(tokenAuthentication);

        Assertions.assertTrue(serviceUnderTest.isValid(new JwtAuthSource("jwtToken")));
        verify(authenticationService, times(1)).validateJwtToken("jwtToken");
    }

    @Test
    void givenInvalidAuthSource_whenIsValid_thenFalse() {
        TokenAuthentication tokenAuthentication = new TokenAuthentication("user");
        tokenAuthentication.setAuthenticated(false);
        when(authenticationService.validateJwtToken(anyString())).thenReturn(tokenAuthentication);

        Assertions.assertFalse(serviceUnderTest.isValid(new JwtAuthSource("jwtToken")));
        verify(authenticationService, times(1)).validateJwtToken("jwtToken");
    }

    @Test
    void givenTokenNotValidException_whenIsValid_thenThrow() {
        JwtAuthSource authSource = new JwtAuthSource("jwtToken");
        when(authenticationService.validateJwtToken(anyString())).thenThrow(new TokenNotValidException("token not valid"));

        assertThrows(TokenNotValidException.class, () -> serviceUnderTest.isValid(authSource));
        verify(authenticationService, times(1)).validateJwtToken("jwtToken");
    }

    @Test
    void givenTokenExpireException_whenIsValid_thenThrow() {
        JwtAuthSource authSource = new JwtAuthSource("jwtToken");
        when(authenticationService.validateJwtToken(anyString())).thenThrow(new TokenExpireException("token expired"));

        assertThrows(TokenExpireException.class, () -> serviceUnderTest.isValid(authSource));
        verify(authenticationService, times(1)).validateJwtToken("jwtToken");
    }

    @Test
    void givenNullAuthSource_whenParse_thenNull() {
        Assertions.assertNull(serviceUnderTest.parse(null));
        verifyNoInteractions(authenticationService);
    }

    @Test
    void givenNullTokenInAuthSource_whenParse_thenFalse() {
        Assertions.assertNull(serviceUnderTest.parse(new JwtAuthSource(null)));
        verifyNoInteractions(authenticationService);
    }

    @Test
    void givenUnknownAuthSource_whenParse_thenFalse() {
        Assertions.assertNull(serviceUnderTest.parse(new DummyAuthSource()));
        verifyNoInteractions(authenticationService);
    }

    @Test
    void givenValidAuthSource_wheParse_thenTrue() {
        Parsed expectedParsedSource = new Parsed("user", new Date(111), new Date(222), Source.ZOSMF);
        when(authenticationService.parseJwtToken(anyString())).thenReturn(new QueryResponse("domain", "user", new Date(111), new Date(222), Source.ZOSMF));

        Parsed parsedSource = serviceUnderTest.parse(new JwtAuthSource("jwtToken"));

        verify(authenticationService, times(1)).parseJwtToken("jwtToken");
        Assertions.assertNotNull(parsedSource);
        Assertions.assertEquals(expectedParsedSource, parsedSource);
    }

    @Test
    void givenTokenNotValidException_whenParse_thenThrow() {
        JwtAuthSource authSource = new JwtAuthSource("jwtToken");
        when(authenticationService.parseJwtToken(anyString())).thenThrow(new TokenNotValidException("token not valid"));

        assertThrows(TokenNotValidException.class, () -> serviceUnderTest.parse(authSource));
        verify(authenticationService, times(1)).parseJwtToken("jwtToken");
    }

    @Test
    void givenTokenExpireException_whenParse_thenThrow() {
        JwtAuthSource authSource = new JwtAuthSource("jwtToken");
        when(authenticationService.parseJwtToken(anyString())).thenThrow(new TokenExpireException("token expired"));

        assertThrows(TokenExpireException.class, () -> serviceUnderTest.parse(authSource));
        verify(authenticationService, times(1)).parseJwtToken("jwtToken");
    }

    @Test
    void givenNullAuthSource_whenGetLtpaToken_thenNull() {
        Assertions.assertNull(serviceUnderTest.getLtpaToken(null));
        verifyNoInteractions(authenticationService);
    }

    @Test
    void givenNullTokenInAuthSource_whenGetLtpaToken_thenNull() {
        Assertions.assertNull(serviceUnderTest.getLtpaToken(new JwtAuthSource(null)));
        verifyNoInteractions(authenticationService);
    }

    @Test
    void givenUnknownAuthSource_whenGetLtpaToken_thenNull() {
        Assertions.assertNull(serviceUnderTest.getLtpaToken(new DummyAuthSource()));
        verifyNoInteractions(authenticationService);
    }

    @Test
    void givenValidAuthSource_whenGetLtpaToken_thenTokenGenerated() {
        String ltpa = "ltpaToken";
        when(authenticationService.getLtpaTokenWithValidation(anyString())).thenReturn(ltpa);

        Assertions.assertEquals(ltpa, serviceUnderTest.getLtpaToken(new JwtAuthSource("jwtToken")));
        verify(authenticationService, times(1)).getLtpaTokenWithValidation("jwtToken");
    }

    @Test
    void givenTokenNotValidException_whenGetLtpaToken_thenThrow() {
        JwtAuthSource authSource = new JwtAuthSource("jwtToken");
        when(authenticationService.getLtpaTokenWithValidation(anyString())).thenThrow(new TokenNotValidException("token not valid"));

        assertThrows(TokenNotValidException.class, () -> serviceUnderTest.getLtpaToken(authSource));
        verify(authenticationService, times(1)).getLtpaTokenWithValidation("jwtToken");
    }

    @Test
    void givenTokenExpireException_whenGetLtpaToken_thenThrow() {
        JwtAuthSource authSource = new JwtAuthSource("jwtToken");
        when(authenticationService.getLtpaTokenWithValidation(anyString())).thenThrow(new TokenExpireException("token expired"));

        assertThrows(TokenExpireException.class, () -> serviceUnderTest.getLtpaToken(authSource));
        verify(authenticationService, times(1)).getLtpaTokenWithValidation("jwtToken");
    }


    static class DummyAuthSource implements AuthSource {
        @Override
        public Object getSource() {
            return null;
        }
    }
}
