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
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EurekaDashBoardRedirectFilterTest {

    @Mock private FilterChain chain;

    @InjectMocks
    private EurekaDashBoardRedirectFilter filter;

    @BeforeEach
    void setUp() throws IOException, ServletException {
        lenient().doNothing().when(chain).doFilter(any(), any());
        ReflectionTestUtils.setField(filter, "externalPort", 10010);
    }

    @Nested
    class WhenUsingExternalPort {

        @Test
        void whenFilter_thenContinue() throws IOException, ServletException {
            var request = new MockHttpServletRequest();
            request.setServerPort(10010);

            var response = mock(HttpServletResponse.class);

            filter.doFilter(request, response, chain);

            verifyNoInteractions(response);
            verify(chain, times(1)).doFilter(request, response);
        }

    }

    @Nested
    class WhenUsingInternalDiscoveryPort {

        @Test
        void whenFilterWithHomePage_thenForwardEurekaHomePage() throws IOException, ServletException {
            var response = mock(HttpServletResponse.class);
            var request = mock(HttpServletRequest.class);
            var session = mock(HttpSession.class);
            var servletContext = mock(ServletContext.class);
            var requestDispatcher = mock(RequestDispatcher.class);
            when(request.getServerPort()).thenReturn(10011);
            when(request.getRequestURI()).thenReturn("/");
            when(request.getSession()).thenReturn(session);
            when(session.getServletContext()).thenReturn(servletContext);
            when(servletContext.getRequestDispatcher("/eureka")).thenReturn(requestDispatcher);
            doNothing().when(requestDispatcher).forward(request, response);

            filter.doFilter(request, response, chain);

            verifyNoInteractions(response);
        }

        @Test
        void whenFilterWithOtherURL_thenContinue() throws IOException, ServletException {
            var request = new MockHttpServletRequest();
            request.setServerPort(10011);

            var response = mock(HttpServletResponse.class);

            filter.doFilter(request, response, chain);

            verifyNoInteractions(response);
            verify(chain, times(1)).doFilter(request, response);
        }

    }

}
