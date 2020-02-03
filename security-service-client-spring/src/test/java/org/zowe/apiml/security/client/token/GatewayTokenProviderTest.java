/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.client.token;

import org.zowe.apiml.security.client.service.GatewaySecurityService;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.security.core.Authentication;

import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GatewayTokenProviderTest {
    private static final String USER = "USER";
    private static final String DOMAIN = "PASS";
    private static final String VALID_TOKEN = "VALID_TOKEN";

    private final GatewaySecurityService gatewaySecurityService = mock(GatewaySecurityService.class);
    private final GatewayTokenProvider gatewayTokenProvider = new GatewayTokenProvider(gatewaySecurityService);

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Test
    public void shouldAuthenticateValidToken() {
        when(gatewaySecurityService.query(VALID_TOKEN)).thenReturn(new QueryResponse(DOMAIN, USER, new Date(), new Date()));
        TokenAuthentication tokenAuthentication = new TokenAuthentication(VALID_TOKEN);

        Authentication processedAuthentication = gatewayTokenProvider.authenticate(tokenAuthentication);

        assertTrue(processedAuthentication instanceof TokenAuthentication);
        assertTrue(processedAuthentication.isAuthenticated());
        assertEquals(VALID_TOKEN, processedAuthentication.getCredentials());
        assertEquals(USER, processedAuthentication.getName());
    }

    @Test
    public void shouldSupportTokenAuthentication() {
        assertTrue(gatewayTokenProvider.supports(TokenAuthentication.class));
    }

    @Test
    public void shouldNotSupportGenericAuthentication() {
        assertFalse(gatewayTokenProvider.supports(Authentication.class));
    }
}
