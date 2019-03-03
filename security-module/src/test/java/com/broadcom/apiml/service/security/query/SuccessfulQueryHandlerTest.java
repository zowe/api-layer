/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.service.security.query;

import com.broadcom.apiml.service.security.config.SecurityConfigurationProperties;
import com.broadcom.apiml.service.security.token.TokenAuthentication;
import com.broadcom.apiml.service.security.token.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore
public class SuccessfulQueryHandlerTest {

    @Test
    public void successfulLoginHandlerTestForCookie() throws IOException {
        String user = "user";
        String domain = "domain";
        String ltpa = "ltpa";
        String secret = "secret";

        SecurityConfigurationProperties securityConfigurationProperties = new SecurityConfigurationProperties();
        TokenService tokenService = new TokenService(securityConfigurationProperties);
        tokenService.setSecret(secret);
        ObjectMapper mapper = new ObjectMapper();
        SuccessfulQueryHandler successfulQueryHandler = new SuccessfulQueryHandler(mapper, tokenService);

        String token = tokenService.createToken(user, domain, ltpa);
        TokenAuthentication tokenAuthentication = new TokenAuthentication(user, token);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        successfulQueryHandler.onAuthenticationSuccess(request, response, tokenAuthentication);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, response.getContentType());
        assertTrue(response.getContentAsString().contains("{\"domain\":\"domain\",\"userId\":\"user\",\"creation\":"));
    }
}
