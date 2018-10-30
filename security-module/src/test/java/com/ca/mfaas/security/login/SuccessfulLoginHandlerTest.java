/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.security.login;

import com.ca.mfaas.security.token.CookieConfiguration;
import com.ca.mfaas.security.token.TokenAuthentication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SuccessfulLoginHandlerTest {

    @Test
    public void successfulLoginHandlerTest() throws IOException {
        ObjectMapper mapper = mock(ObjectMapper.class);
        CookieConfiguration cookieConfiguration = mock(CookieConfiguration.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        TokenAuthentication tokenAuthentication = mock(TokenAuthentication.class);
        when(cookieConfiguration.getName()).thenReturn("apimlAuthenticationToken");

        SuccessfulLoginHandler successfulLoginHandler = new SuccessfulLoginHandler(mapper, cookieConfiguration);
        successfulLoginHandler.onAuthenticationSuccess(request, response, tokenAuthentication);

        verify(response).addCookie(any(Cookie.class));
        verify(tokenAuthentication).getCredentials();
    }

}
