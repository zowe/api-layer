/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.login;

import com.ca.mfaas.gateway.security.token.TokenAuthentication;
import com.ca.mfaas.product.config.MFaaSConfigPropertiesContainer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SuccessfulLoginHandlerTest {

    @Test
    public void successfulLoginHandlerTestForCookie() throws IOException {
        ObjectMapper mapper = mock(ObjectMapper.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MFaaSConfigPropertiesContainer propertiesContainer = new MFaaSConfigPropertiesContainer();
        propertiesContainer.setSecurity(new MFaaSConfigPropertiesContainer.SecurityProperties());
        request.addHeader(propertiesContainer.getSecurity().getAuthenticationResponseTypeHeaderName(), "cookie");
        TokenAuthentication tokenAuthentication = mock(TokenAuthentication.class);
        SuccessfulLoginHandler successfulLoginHandler = new SuccessfulLoginHandler(mapper, propertiesContainer);
        successfulLoginHandler.onAuthenticationSuccess(request, response, tokenAuthentication);
        verify(response).addCookie(any(Cookie.class));
    }

}
