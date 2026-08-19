/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ContentTypeFilterTest {

    private ContentTypeFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new ContentTypeFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    private void assertPassedThrough() {
        assertSame(request, chain.getRequest());
        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    private void assertRejected() {
        assertNull(chain.getRequest());
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), response.getStatus());
    }

    @Nested
    class GivenRequestWithoutBody {

        @Test
        void whenNoContentType_thenRequestPassesThrough() throws ServletException, IOException {
            filter.doFilter(request, response, chain);
            assertPassedThrough();
        }

        @Test
        void whenContentTypeIsNotJson_thenRequestPassesThrough() throws ServletException, IOException {
            request.setContentType(MediaType.TEXT_PLAIN_VALUE);
            filter.doFilter(request, response, chain);
            assertPassedThrough();
        }

        @Test
        void whenContentLengthIsZero_thenRequestPassesThroughRegardlessOfContentType() throws ServletException, IOException {
            request.setContent(new byte[0]);
            request.setContentType(MediaType.TEXT_PLAIN_VALUE);
            filter.doFilter(request, response, chain);
            assertPassedThrough();
        }
    }

    @Nested
    class GivenRequestWithBodyViaContentLength {

        @BeforeEach
        void setUp() {
            request.setContent("{}".getBytes());
        }

        @Test
        void whenContentTypeIsApplicationJson_thenRequestPassesThrough() throws ServletException, IOException {
            request.setContentType(MediaType.APPLICATION_JSON_VALUE);
            filter.doFilter(request, response, chain);
            assertPassedThrough();
        }

        @Test
        void whenContentTypeIsApplicationJsonWithCharset_thenRequestPassesThrough() throws ServletException, IOException {
            request.setContentType("application/json;charset=UTF-8");
            filter.doFilter(request, response, chain);
            assertPassedThrough();
        }

        @Test
        void whenContentTypeIsMissing_thenRejectedWithUnsupportedMediaType() throws ServletException, IOException {
            filter.doFilter(request, response, chain);
            assertRejected();
        }

        @Test
        void whenContentTypeIsNotJson_thenRejectedWithUnsupportedMediaType() throws ServletException, IOException {
            request.setContentType(MediaType.TEXT_PLAIN_VALUE);
            filter.doFilter(request, response, chain);
            assertRejected();
        }

        @Test
        void whenContentTypeIsJsonVariant_thenRejectedWithUnsupportedMediaType() throws ServletException, IOException {
            request.setContentType("application/hal+json");
            filter.doFilter(request, response, chain);
            assertRejected();
        }

        @Test
        void whenContentTypeIsMalformed_thenRejectedWithUnsupportedMediaType() throws ServletException, IOException {
            request.setContentType("not-a-valid-media-type");
            filter.doFilter(request, response, chain);
            assertRejected();
        }
    }

    @Nested
    class GivenRequestWithBodyViaTransferEncoding {

        @BeforeEach
        void setUp() {
            request.addHeader(HttpHeaders.TRANSFER_ENCODING, "chunked");
        }

        @Test
        void whenContentTypeIsApplicationJson_thenRequestPassesThrough() throws ServletException, IOException {
            request.setContentType(MediaType.APPLICATION_JSON_VALUE);
            filter.doFilter(request, response, chain);
            assertPassedThrough();
        }

        @Test
        void whenContentTypeIsMissing_thenRejectedWithUnsupportedMediaType() throws ServletException, IOException {
            filter.doFilter(request, response, chain);
            assertRejected();
        }
    }
}
