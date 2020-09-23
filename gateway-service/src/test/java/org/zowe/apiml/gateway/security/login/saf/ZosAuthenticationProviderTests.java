
/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.login.saf;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.gateway.security.login.saf.MockPlatformUser.*;

class ZosAuthenticationProviderTests {
    private static ZosAuthenticationProvider provider;
    private static AuthenticationService mockService;

    private UsernamePasswordAuthenticationToken VALID_TOKEN = new UsernamePasswordAuthenticationToken(VALID_USERID,
        VALID_PASSWORD);
    private UsernamePasswordAuthenticationToken INVALID_TOKEN = new UsernamePasswordAuthenticationToken(INVALID_USERID,
        INVALID_PASSWORD);

    @BeforeAll
    static void setup() {
        mockService = mock(AuthenticationService.class);
        provider = new ZosAuthenticationProvider(mockService);
        provider.afterPropertiesSet();
    }

    @Test
    void exceptionOnInvalidCredentials() {
        assertThrows(BadCredentialsException.class,
            () -> provider.authenticate(INVALID_TOKEN),
            "Expected exception is not ZosAuthenticationException");
    }

    @Test
    void validAuthenticationOnOnValidCredentials() {
        String validJwtToken = "validJwtToken";
        when(mockService.createJwtToken(anyString(), anyString(), any())).thenReturn(validJwtToken);
        when(mockService.createTokenAuthentication(VALID_USERID, validJwtToken))
            .thenReturn(new TokenAuthentication(VALID_USERID, validJwtToken));

        Authentication authentication = provider.authenticate(VALID_TOKEN);
        assertThat(VALID_USERID, is(authentication.getPrincipal()));
    }

    @Test
    void supportsUsernamePasswordAuthenticationToken() {
        boolean supportsValidToken = provider.supports(VALID_TOKEN.getClass());
        assertThat(supportsValidToken, is(true));
    }
}
