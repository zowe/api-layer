package org.zowe.apiml.gateway.security.config;/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import org.junit.Test;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class CertificateAuthenticationProviderTest {

    @Test
    public void authenticate() {
        CertificateAuthenticationProvider cap = new CertificateAuthenticationProvider();
        Authentication authentication = mock(Authentication.class);
        cap.authenticate(authentication);
        verify(authentication, times(1)).setAuthenticated(true);
    }

    @Test
    public void supports() {
        CertificateAuthenticationProvider cap = new CertificateAuthenticationProvider();
        assertTrue(cap.supports(PreAuthenticatedAuthenticationToken.class));
        assertFalse(cap.supports(AuthenticationProvider.class));
    }
}
