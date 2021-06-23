/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.login;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ShouldBeAlreadyAuthenticatedFilterTest {

    @Test
    void throwsEveryTime() {
        AuthenticationFailureHandler handler = mock(AuthenticationFailureHandler.class);
        ShouldBeAlreadyAuthenticatedFilter underTest = new ShouldBeAlreadyAuthenticatedFilter("/", handler);
        assertThrows(AuthenticationCredentialsNotFoundException.class,
            () -> underTest.attemptAuthentication(mock(HttpServletRequest.class), mock(HttpServletResponse.class)));
    }
}
