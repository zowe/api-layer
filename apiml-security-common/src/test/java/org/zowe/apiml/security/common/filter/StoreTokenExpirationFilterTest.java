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
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class StoreTokenExpirationFilterTest {
    StoreTokenExpirationFilter underTest;
    private MockHttpServletRequest request;
    private ServletResponse response;
    private FilterChain chain;
    private static final String VALID_JSON = "{\"validity\": 90, \"scopes\": [\"service\"]}";

    @BeforeEach
    public void setUp() {
        request = new MockHttpServletRequest();
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        underTest = new StoreTokenExpirationFilter();
        request.setMethod(HttpMethod.POST.name());
        request.setContent(VALID_JSON.getBytes());
    }

    @Nested
    class GivenRequestWithBody {

        @Test
        void thenStoreExpirationTime() throws ServletException, IOException {
            underTest.doFilter(request, response, chain);
            String expirationTime = request.getAttribute("expirationTime").toString();
            assertNotNull(expirationTime);
            assertEquals("90", expirationTime);
            verify(chain, times(1)).doFilter(request, response);
        }
    }
}
