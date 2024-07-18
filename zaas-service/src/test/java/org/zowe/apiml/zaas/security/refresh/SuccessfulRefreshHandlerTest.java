/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.refresh;

import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import org.zowe.apiml.zaas.security.service.TokenCreationService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

class SuccessfulRefreshHandlerTest {

    AuthConfigurationProperties authConfigurationProperties = new AuthConfigurationProperties();
    AuthenticationService authenticationService = mock(AuthenticationService.class);
    TokenCreationService tokenCreationService = mock(TokenCreationService.class);
    SuccessfulRefreshHandler underTest = new SuccessfulRefreshHandler(authConfigurationProperties,
        authenticationService, tokenCreationService);
    HttpServletRequest request;
    HttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        when(tokenCreationService.createJwtTokenWithoutCredentials(anyString())).thenReturn("NEWTOKEN");
    }

    @Nested
    class GivenAuthenticationInputs {
        @Test
        void unknownTypeOfAuthenticationDoesntDoAnything() throws ServletException, IOException {
            Authentication auth = new TestingAuthenticationToken("Principal", "credentials");
            underTest.onAuthenticationSuccess(request, response, auth);
            verify(authenticationService, never()).invalidateJwtToken("TOKEN", true);
            assertThat(response.getStatus(), is(HttpStatus.NO_CONTENT.value()));
            assertThat(response.getHeader(HttpHeaders.SET_COOKIE), is(emptyOrNullString()));
        }

        @Test
        void tokenTypeOfAuthenticationIssuesToken() throws ServletException, IOException {
            Authentication auth = new TokenAuthentication("USER", "TOKEN");
            underTest.onAuthenticationSuccess(request, response, auth);
            verify(authenticationService, atLeastOnce()).invalidateJwtToken("TOKEN", true);
            assertThat(response.getStatus(), is(HttpStatus.NO_CONTENT.value()));
            assertThat(response.getHeader(HttpHeaders.SET_COOKIE),
                is("apimlAuthenticationToken=NEWTOKEN; Path=/; Secure; HttpOnly; SameSite=Strict"));
        }
    }

}
