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

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ContentTypeFilterTest {

    private ContentTypeFilter filter;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new ContentTypeFilter();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @Nested
    class WhenRequestHasBody {

        @Test
        void givenJsonContentType_thenRequestProceeds() throws ServletException, IOException {
            var request = new MockHttpServletRequest("POST", "/auth/login");
            request.setContentType(MediaType.APPLICATION_JSON_VALUE);
            request.setContent("{}".getBytes(StandardCharsets.UTF_8));

            filter.doFilter(request, response, chain);

            assertEquals(request, chain.getRequest());
        }

        @Test
        void givenNoContentType_thenRejectedWithUnsupportedMediaType() throws ServletException, IOException {
            var request = new MockHttpServletRequest("POST", "/auth/login");
            request.setContent("{}".getBytes(StandardCharsets.UTF_8));

            filter.doFilter(request, response, chain);

            assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), response.getStatus());
            assertNull(chain.getRequest());
        }

        @Test
        void givenNonJsonContentType_thenRejectedWithUnsupportedMediaType() throws ServletException, IOException {
            var request = new MockHttpServletRequest("POST", "/auth/login");
            request.setContentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            request.setContent("username=a&password=b".getBytes(StandardCharsets.UTF_8));

            filter.doFilter(request, response, chain);

            assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), response.getStatus());
            assertNull(chain.getRequest());
        }

        @Test
        void givenChunkedTransferEncodingAndNoContentType_thenRejectedWithUnsupportedMediaType() throws ServletException, IOException {
            var request = new MockHttpServletRequest("POST", "/auth/login");
            request.addHeader(HttpHeaders.TRANSFER_ENCODING, "chunked");

            filter.doFilter(request, response, chain);

            assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), response.getStatus());
            assertNull(chain.getRequest());
        }

    }

    @Nested
    class WhenRequestHasNoBody {

        @Test
        void givenNoContentTypeAndNoBody_thenRequestProceeds() throws ServletException, IOException {
            var request = new MockHttpServletRequest("POST", "/auth/logout");

            filter.doFilter(request, response, chain);

            assertEquals(request, chain.getRequest());
        }

    }

}
