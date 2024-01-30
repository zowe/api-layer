/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gzip;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GZipResponseUtilsTest {

    @Test
    void addHeader() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        GZipResponseUtils.addGzipHeader(response);
        assertEquals("gzip", response.getHeader("Content-Encoding"));
    }

    @Test
    void whenSetHeaderFails_thenThrowException() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.containsHeader("Content-Encoding")).thenReturn(false);
        assertThrows(GZipResponseException.class, () -> GZipResponseUtils.addGzipHeader(response));
    }

    @Test
    void whenContentShouldBeEmpty_thenReturnTrue() {
        assertFalse(GZipResponseUtils.shouldBodyBeZero(200));
        assertTrue(GZipResponseUtils.shouldBodyBeZero(204));
        assertTrue(GZipResponseUtils.shouldBodyBeZero(304));
    }

    @Test
    void whenGZippedBodyIsEmpty_thenReturnTrue() {
        byte[] bytes = new byte[20];
        assertTrue(GZipResponseUtils.shouldGzippedBodyBeZero(bytes));
    }
}
