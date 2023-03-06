/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.refresh;

import org.apache.tomcat.util.http.SameSiteCookies;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.TokenCreationService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.security.common.utils.SecurityUtils.COOKIE_NAME;

class SuccessfulRefreshHandlerTest {

    AuthConfigurationProperties authConfigurationProperties = mock(AuthConfigurationProperties.class);
    AuthenticationService authenticationService = mock(AuthenticationService.class);
    TokenCreationService tokenCreationService = mock(TokenCreationService.class);
    AuthConfigurationProperties.CookieProperties cookieProperties = mock(AuthConfigurationProperties.CookieProperties.class);
    SuccessfulRefreshHandler underTest = new SuccessfulRefreshHandler(authConfigurationProperties, authenticationService, tokenCreationService);
    HttpServletRequest request;
    HttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        when(tokenCreationService.createJwtTokenWithoutCredentials(anyString())).thenReturn("NEWTOKEN");
        when(authConfigurationProperties.getCookieProperties()).thenReturn(cookieProperties);
        authConfigurationProperties.getCookieProperties().setCookieComment("");
        authConfigurationProperties.getCookieProperties().setCookieNamePAT("PAT");
        authConfigurationProperties.getCookieProperties().setCookieMaxAge(null);
        authConfigurationProperties.getCookieProperties().setCookieSecure(true);
        authConfigurationProperties.getCookieProperties().setCookieSameSite(SameSiteCookies.STRICT);
        when(authConfigurationProperties.getCookieProperties().getCookieSameSite()).thenReturn(SameSiteCookies.STRICT);
        when(authConfigurationProperties.getCookieProperties().getCookiePath()).thenReturn("/");
        when(authConfigurationProperties.getCookieProperties().isCookieSecure()).thenReturn(true);
        when(authConfigurationProperties.getCookieProperties().getCookieMaxAge()).thenReturn(null);
        when(authConfigurationProperties.getCookieProperties().getCookieName()).thenReturn(COOKIE_NAME);

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
            assertThat(response.getHeader(HttpHeaders.SET_COOKIE), is("apimlAuthenticationToken=NEWTOKEN; Path=/; Secure; HttpOnly; SameSite=Strict"));
        }
    }

}
