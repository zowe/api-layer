/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.client.login;

import org.zowe.apiml.security.client.service.GatewaySecurityService;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatewayLoginProviderTest {

    private static final String USER = "USER";
    private static final String VALID_PASSWORD = "PASS";
    private static final String INVALID_PASSWORD = "WORD";
    private static final String VALID_TOKEN = "VALID_TOKEN";

    private final GatewaySecurityService gatewaySecurityService = mock(GatewaySecurityService.class);
    private final GatewayLoginProvider gatewayLoginProvider = new GatewayLoginProvider(gatewaySecurityService);

    @Test
    void shouldAuthenticateValidUsernamePassword() {
        when(gatewaySecurityService.login(USER, VALID_PASSWORD)).thenReturn(Optional.of(VALID_TOKEN));

        Authentication auth = new UsernamePasswordAuthenticationToken(USER, VALID_PASSWORD);
        Authentication processedAuthentication = gatewayLoginProvider.authenticate(auth);

        assertTrue(processedAuthentication instanceof TokenAuthentication);
        assertTrue(processedAuthentication.isAuthenticated());
        assertEquals(VALID_TOKEN, processedAuthentication.getCredentials());
        assertEquals(USER, processedAuthentication.getName());
    }

    @Test
    void shouldThrowWithInvalidUsernamePassword() {
        when(gatewaySecurityService.login(USER, INVALID_PASSWORD)).thenReturn(Optional.empty());

        Authentication auth = new UsernamePasswordAuthenticationToken(USER, INVALID_PASSWORD);
        assertThrows(BadCredentialsException.class, () -> gatewayLoginProvider.authenticate(auth));
    }

    @Test
    void shouldSupportUsernamePasswordAuthentication() {
        assertTrue(gatewayLoginProvider.supports(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void shouldNotSupportGenericAuthentication() {
        assertFalse(gatewayLoginProvider.supports(Authentication.class));
        assertFalse(gatewayLoginProvider.supports(TokenAuthentication.class));
    }
}
