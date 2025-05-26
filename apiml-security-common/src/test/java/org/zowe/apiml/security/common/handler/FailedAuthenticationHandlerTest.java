/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.handler;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.security.common.error.AuthExceptionHandler;

import java.io.IOException;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


class FailedAuthenticationHandlerTest {

    @Test
    void testOnAuthenticationFailure() throws ServletException, IOException {
        AuthExceptionHandler authExceptionHandler = mock(AuthExceptionHandler.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FailedAuthenticationHandler handler = new FailedAuthenticationHandler(authExceptionHandler);
        BadCredentialsException exception = new BadCredentialsException("ERROR");

        ArgumentCaptor<BiConsumer<ApiMessageView, HttpStatus>> consumerCaptor = ArgumentCaptor.forClass(BiConsumer.class);
        ArgumentCaptor<BiConsumer<String, String>> headerCaptor = ArgumentCaptor.forClass(BiConsumer.class);
        ArgumentCaptor<RuntimeException> exceptionCaptor = ArgumentCaptor.forClass(RuntimeException.class);

        handler.onAuthenticationFailure(request, response, exception);

        verify(authExceptionHandler).handleException(
            eq(request.getRequestURI()),
            consumerCaptor.capture(),
            headerCaptor.capture(),
            exceptionCaptor.capture()
        );

        assertEquals(BadCredentialsException.class, exceptionCaptor.getValue().getClass());
        assertEquals("ERROR", exceptionCaptor.getValue().getMessage());
    }

}
