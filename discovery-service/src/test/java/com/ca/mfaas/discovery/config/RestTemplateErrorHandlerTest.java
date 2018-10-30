/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.discovery.config;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RestTemplateErrorHandlerTest {

    private RestTemplateErrorHandler errorHandler;

    @Before
    public void setUp() {
        errorHandler = new RestTemplateErrorHandler();
    }

    @Test
    public void handleError() throws IOException {
        ClientHttpResponse response = mock(ClientHttpResponse.class);

        errorHandler.handleError(response);

        verify(response, times(1)).getStatusCode();
        verify(response, times(1)).getStatusText();
    }

    @Test
    public void hasError() throws IOException {
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenReturn(HttpStatus.BAD_GATEWAY);

        boolean result = errorHandler.hasError(response);

        assertTrue(result);
        verify(response, times(1)).getStatusCode();
    }

    @Test
    public void hasNoError() throws IOException {
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);

        boolean result = errorHandler.hasError(response);

        assertFalse(result);
        verify(response, times(1)).getStatusCode();
    }
}
