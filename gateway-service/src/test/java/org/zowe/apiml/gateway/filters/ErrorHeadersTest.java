/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.zowe.apiml.constants.ApimlConstants;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.*;

class ErrorHeadersTest {

    @Test
    void testEmptyConstructor_createsEmptyHeaders() {
        ErrorHeaders headers = new ErrorHeaders();

        assertTrue(headers.asHttpHeaders().isEmpty());
        assertEquals(OptionalLong.empty(), headers.contentLength());
        assertEquals(Optional.empty(), headers.contentType());
        assertEquals(List.of(), headers.header(ApimlConstants.AUTH_FAIL_HEADER));
    }

    @Test
    void testConstructorWithMessage_addsAuthFailureHeader() {
        String failureMessage = "Invalid token";
        ErrorHeaders headers = new ErrorHeaders(failureMessage);

        HttpHeaders httpHeaders = headers.asHttpHeaders();
        assertTrue(httpHeaders.containsKey(ApimlConstants.AUTH_FAIL_HEADER));
        assertEquals(List.of(failureMessage), httpHeaders.get(ApimlConstants.AUTH_FAIL_HEADER));
    }

    @Test
    void testHeaderReturnsEmptyListIfNotPresent() {
        ErrorHeaders headers = new ErrorHeaders("Some error");

        List<String> values = headers.header("Non-Existing-Header");
        assertNotNull(values);
        assertTrue(values.isEmpty());
    }

    @Test
    void testContentLengthWithExplicitValue() {
        ErrorHeaders headers = new ErrorHeaders();
        headers.asHttpHeaders().setContentLength(123L);

        assertEquals(OptionalLong.of(123L), headers.contentLength());
    }

    @Test
    void testContentTypeReturnsValueIfSet() {
        ErrorHeaders headers = new ErrorHeaders();
        headers.asHttpHeaders().setContentType(MediaType.APPLICATION_JSON);

        assertEquals(Optional.of(MediaType.APPLICATION_JSON), headers.contentType());
    }
}
