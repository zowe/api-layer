
/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service.saf;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.zowe.zos.security.MockPlatformUser.*;

public class ZosAuthenticationProviderTests {
    private static ZosAuthenticationProvider provider = new ZosAuthenticationProvider();
    private UsernamePasswordAuthenticationToken VALID_TOKEN = new UsernamePasswordAuthenticationToken(VALID_USERID,
            VALID_PASSWORD);
    private UsernamePasswordAuthenticationToken INVALID_TOKEN = new UsernamePasswordAuthenticationToken(INVALID_USERID,
            INVALID_PASSWORD);

    @BeforeClass
    public static void setup() throws Exception {
        provider.afterPropertiesSet();
    }

    @Test(expected = ZosAuthenticationException.class)
    public void exceptionOnInvalidCredentials() {
        provider.authenticate(INVALID_TOKEN);
    }

    @Test
    public void validAuthenticationOnOnValidCredentials() {
        Authentication authentication = provider.authenticate(VALID_TOKEN);
        assertEquals(authentication.getPrincipal(), VALID_USERID);
    }

    @Test
    public void supportsUsernamePasswordAuthenticationToken() {
        provider.supports(VALID_TOKEN.getClass());
    }

}
