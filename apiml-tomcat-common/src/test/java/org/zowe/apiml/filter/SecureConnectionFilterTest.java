/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.commons.attls.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecureConnectionFilterTest {

    FilterChain chain = mock(FilterChain.class);
    HttpServletResponse response = new MockHttpServletResponse();

    @Nested
    class Success {
        @Test
        void whenConnectionIsSecure() throws IOException, ServletException, UnknownEnumValueException, IoctlCallException {
            SecureConnectionFilter filter = new SecureConnectionFilter();
            AttlsContext context = mock(AttlsContext.class);
            ThreadLocal<AttlsContext> contexts = new ThreadLocal<>();
            contexts.set(context);
            ReflectionTestUtils.setField(InboundAttls.class, "contexts", contexts);
            when(context.getStatConn()).thenReturn(StatConn.SECURE);
            filter.doFilterInternal(new MockHttpServletRequest(), response, chain);
            assertEquals(200, response.getStatus());
            InboundAttls.dispose();
        }
    }

    @Nested
    class WhenErrorOccurs {
        @Test
        void whenConnectionIsNotSecure_thenRespondWithErrorCode() throws IOException, ServletException, UnknownEnumValueException, IoctlCallException {
            SecureConnectionFilter filter = new SecureConnectionFilter();
            AttlsContext context = mock(AttlsContext.class);
            ThreadLocal<AttlsContext> contexts = new ThreadLocal<>();
            contexts.set(context);
            ReflectionTestUtils.setField(InboundAttls.class, "contexts", contexts);
            when(context.getStatConn()).thenReturn(StatConn.NOTSECURE);
            filter.doFilterInternal(new MockHttpServletRequest(), response, chain);
            assertEquals(500, response.getStatus());
            InboundAttls.dispose();
        }

        @Test
        void whenContextIsNotInitialized_thenRespondWithErrorCode() throws IOException, ServletException {
            SecureConnectionFilter filter = new SecureConnectionFilter();
            filter.doFilterInternal(new MockHttpServletRequest(), response, chain);
            assertEquals(500, response.getStatus());
        }
    }

}
