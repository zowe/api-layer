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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.security.common.token.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RequestAuthenticationServiceTest {
    String VALID_USER = "tom";

    AuthenticationService authenticationService = mock(AuthenticationService.class);
    RequestAuthenticationService underTest = new RequestAuthenticationService(authenticationService);
    HttpServletRequest request = mock(HttpServletRequest.class);

    @Nested
    class WhenGettingAuthenticatedUser {
        @Nested
        class ReturnEmpty {
            @Test
            void givenNoJwtToken() {
                when(authenticationService.getJwtTokenFromRequest(any())).thenReturn(Optional.empty());

                Optional<String> user = underTest.getAuthenticatedUser(request);
                assertThat(user.isPresent(), is(false));
            }

            @Test
            void givenInvalidToken() {
                String invalidToken = "invalidToken";
                when(authenticationService.getJwtTokenFromRequest(any())).thenReturn(Optional.of(invalidToken));
                when(authenticationService.validateJwtToken(invalidToken)).thenReturn(authentication(invalidToken, false));

                Optional<String> user = underTest.getAuthenticatedUser(request);
                assertThat(user.isPresent(), is(false));
            }
        }

        @Nested
        class ReturnUsername {
            @Test
            void givenValidToken() {
                String validToken = "validToken";
                when(authenticationService.getJwtTokenFromRequest(any())).thenReturn(Optional.of(validToken));
                when(authenticationService.validateJwtToken(validToken)).thenReturn(authentication(validToken, true));

                Optional<String> user = underTest.getAuthenticatedUser(request);
                assertThat(user.isPresent(), is(true));
                assertThat(user.get(), is(VALID_USER));
            }
        }
    }

    private TokenAuthentication authentication(String jwtToken, boolean authenticated) {
        TokenAuthentication authentication = new TokenAuthentication(VALID_USER, jwtToken);
        authentication.setAuthenticated(authenticated);
        return authentication;
    }

    @Nested
    class WhenGettingPrincipalFromRequest {

        @Test
        void emptyRequestGivesEmpty() {
            assertThat(underTest.getPrincipalFromRequest(request), is(Optional.empty()));
        }

        @Test
        void emptyWhenTokenNotReturned() {
            doReturn(Optional.empty()).when(authenticationService).getJwtTokenFromRequest(any());
            assertThat(underTest.getPrincipalFromRequest(request), is(Optional.empty()));
        }

        @Test
        void emptyWhenTokenCannotBeParsed() {
            doReturn(Optional.of("jwtTokenString")).when(authenticationService).getJwtTokenFromRequest(any());
            doThrow(TokenNotValidException.class).when(authenticationService).parseJwtToken(any());
            assertThat(underTest.getPrincipalFromRequest(request), is(Optional.empty()));
        }

        @Test
        void principalWithValidToken() {
            doReturn(Optional.of("jwtTokenString")).when(authenticationService).getJwtTokenFromRequest(any());
            doReturn(new QueryResponse("domain", "user", new Date(), new Date(), "issuer", Collections.emptyList(), QueryResponse.Source.ZOSMF)).when(authenticationService).parseJwtToken(any());
            assertThat(underTest.getPrincipalFromRequest(request), is(Optional.of("user")));
        }
    }
}
