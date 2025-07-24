/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.zowe.apiml.security.common.token.OIDCProvider;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.zaas.security.config.CompoundAuthProvider;
import org.zowe.apiml.zaas.security.service.AuthenticationService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GatewaySecurityApiTest {

    @Mock
    private CompoundAuthProvider compoundAuthProvider;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private OIDCProvider oidcProvider;

    @InjectMocks
    private GatewaySecurityApi gatewaySecurityApi;

    @Nested
    class GiveValidAuthentication {

        Authentication authenticated;

        @BeforeEach
        void setup() {
            authenticated = mock(Authentication.class);
            lenient().when(authenticated.isAuthenticated()).thenReturn(true);
        }

        @Test
        void whenLogin_andWhenCredentialsAreValid_thenShouldReturnToken() {
            String expectedToken = "a-valid-jwt-token";
            when(authenticated.getCredentials()).thenReturn(expectedToken);
            when(compoundAuthProvider.authenticate(any(Authentication.class))).thenReturn(authenticated);

            Optional<String> tokenOptional = gatewaySecurityApi.login("user", "password".toCharArray(), null);

            assertTrue(tokenOptional.isPresent(), "Token should be present for a successful login");
            assertEquals(expectedToken, tokenOptional.get(), "The returned token does not match the expected token");
            verify(compoundAuthProvider).authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        void whenLogin_andWhenAuthenticationSucceedsButNoCredentials_thenShouldReturnEmpty() {
            when(authenticated.getCredentials()).thenReturn(null);
            when(compoundAuthProvider.authenticate(any(Authentication.class))).thenReturn(authenticated);

            Optional<String> tokenOptional = gatewaySecurityApi.login("user", "password".toCharArray(), null);

            assertFalse(tokenOptional.isPresent(), "Token should not be present if credentials are not returned");
        }
    }


    @Test
    void givenUsernameIsBlank_thenThrowException() {
        var password = "password".toCharArray();
        assertThrows(AuthenticationCredentialsNotFoundException.class, () ->
            gatewaySecurityApi.login(" ", password, null),
            "Should throw AuthenticationCredentialsNotFoundException for blank username"
        );
    }

    @Test
    void givenPasswordIsEmpty_thenThrowException() {
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            gatewaySecurityApi.login("user", new char[0], null);
        }, "Should throw AuthenticationCredentialsNotFoundException for empty password");
    }

    @Nested
    class GivenValidTokenAuthentication {
        @Test
        void thenReturnQueryResponse() {
            var tokenAuthenticated = mock(TokenAuthentication.class);
            when(tokenAuthenticated.isAuthenticated()).thenReturn(true);

            String validToken = "valid-jwt";
            QueryResponse expectedResponse = new QueryResponse(); // Assuming a default or populated response
            when(authenticationService.validateJwtToken(validToken)).thenReturn(tokenAuthenticated);
            when(authenticationService.parseJwtToken(validToken)).thenReturn(expectedResponse);

            QueryResponse actualResponse = gatewaySecurityApi.query(validToken);

            assertNotNull(actualResponse, "QueryResponse should not be null for a valid token");
            assertEquals(expectedResponse, actualResponse, "The returned QueryResponse does not match the expected one");
            verify(authenticationService).validateJwtToken(validToken);
            verify(authenticationService).parseJwtToken(validToken);
        }
    }

    @Nested
    class GivenInvalidTokenAuthentication {
        @Test
        void whenQuery_andTokenIsInvalid_thenThrowException() {
            var tokenUnAuthenticated = mock(TokenAuthentication.class);
            lenient().when(tokenUnAuthenticated.isAuthenticated()).thenReturn(false);
            String invalidToken = "invalid-jwt";
            when(authenticationService.validateJwtToken(invalidToken)).thenReturn(tokenUnAuthenticated);

            assertThrows(TokenNotValidException.class, () -> {
                gatewaySecurityApi.query(invalidToken);
            }, "Should throw TokenNotValidException for an invalid token");
            verify(authenticationService, never()).parseJwtToken(anyString());
        }

        @Test
        void whenLogin_andAuthenticationFails_thenDoNotReturnAuthentication() {
            var unauthenticated = mock(Authentication.class);
            lenient().when(unauthenticated.isAuthenticated()).thenReturn(false);
            when(compoundAuthProvider.authenticate(any(Authentication.class))).thenReturn(unauthenticated);

            Optional<String> tokenOptional = gatewaySecurityApi.login("user", "password".toCharArray(), null);

            assertFalse(tokenOptional.isPresent(), "Token should not be present for a failed authentication");
        }
    }

    @Test
    void whenVerifyOidc_andTokenIsValid_thenReturnResponse() {
        String validOidcToken = "valid-oidc-token";
        when(oidcProvider.isValid(validOidcToken)).thenReturn(true);

        QueryResponse response = gatewaySecurityApi.verifyOidc(validOidcToken);

        assertNotNull(response, "QueryResponse should not be null for a valid OIDC token");
    }

    @Test
    void whenVerifyOidc_andTokenIsInvalid_thenThrowException() {
        String invalidOidcToken = "invalid-oidc-token";
        when(oidcProvider.isValid(invalidOidcToken)).thenReturn(false);

        assertThrows(TokenNotValidException.class, () -> {
            gatewaySecurityApi.verifyOidc(invalidOidcToken);
        }, "Should throw TokenNotValidException for an invalid OIDC token");
    }

    @Test
    void whenVerifyOidc_andProviderIsNull_thenThrowException() {
        GatewaySecurityApi noOidcApi = new GatewaySecurityApi(compoundAuthProvider, authenticationService, null);
        String token = "any-token";

        assertThrows(TokenNotValidException.class, () -> {
            noOidcApi.verifyOidc(token);
        }, "Should throw TokenNotValidException when OIDC provider is not configured");
    }
}
