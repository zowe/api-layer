/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.config;


import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CertificateAuthenticationProviderTest {

    @Test
    void givenExistingPrincipal_thenReturnAuthenticated() {
        CertificateAuthenticationProvider cap = new CertificateAuthenticationProvider();
        Authentication authentication = mock(PreAuthenticatedAuthenticationToken.class);
        when(authentication.getPrincipal()).thenReturn("user");
        Authentication result = cap.authenticate(authentication);
        assertTrue(result.isAuthenticated());
    }

    @Test
    void supports() {
        CertificateAuthenticationProvider cap = new CertificateAuthenticationProvider();
        assertTrue(cap.supports(PreAuthenticatedAuthenticationToken.class));
        assertFalse(cap.supports(AuthenticationProvider.class));
    }
}
