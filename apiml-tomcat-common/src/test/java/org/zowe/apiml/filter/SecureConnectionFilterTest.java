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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.commons.attls.AttlsContext;
import org.zowe.commons.attls.InboundAttls;
import org.zowe.commons.attls.IoctlCallException;
import org.zowe.commons.attls.StatConn;
import org.zowe.commons.attls.UnknownEnumValueException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class SecureConnectionFilterTest {

    @Mock
    FilterChain chain;

    HttpServletResponse response = new MockHttpServletResponse();

    class AttlsContextTest extends AttlsContext {

        StatConn statConn;

        public AttlsContextTest(StatConn statConn) {
            super(0, false);
            this.statConn = statConn;
        }

        @Override
        public StatConn getStatConn() throws UnknownEnumValueException, IoctlCallException {
            return statConn;
        }
    }

    @Nested
    class Success {
        @Test
        void whenConnectionIsSecure() throws IOException, ServletException, UnknownEnumValueException, IoctlCallException {
            SecureConnectionFilter filter = new SecureConnectionFilter();
            ThreadLocal<AttlsContext> contexts = new ThreadLocal<>();
            contexts.set(new AttlsContextTest(StatConn.SECURE));
            ReflectionTestUtils.setField(InboundAttls.class, "contexts", contexts);
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
            ThreadLocal<AttlsContext> contexts = new ThreadLocal<>();
            contexts.set(new AttlsContextTest(StatConn.NOTSECURE));
            ReflectionTestUtils.setField(InboundAttls.class, "contexts", contexts);
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
