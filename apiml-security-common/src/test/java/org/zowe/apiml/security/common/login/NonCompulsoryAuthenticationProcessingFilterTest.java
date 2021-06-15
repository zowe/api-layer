/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.login;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.function.Supplier;

import static org.mockito.Mockito.*;

class NonCompulsoryAuthenticationProcessingFilterTest {

    private NonCompulsoryAuthenticationProcessingFilter underTest;
    private FilterChain filterChain;
    private MockHttpServletRequest req;
    private MockHttpServletResponse res;


    public static class FilterImpl extends NonCompulsoryAuthenticationProcessingFilter {

        private final Supplier<Authentication> authenticationFunction;

        protected FilterImpl(Supplier<Authentication> authenticationFunction) {
            super("/");
            this.authenticationFunction = authenticationFunction;
        }

        @Override
        public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
            return authenticationFunction.get();
        }
    }

    @BeforeEach
    void setUp() {
        req = new MockHttpServletRequest();
        req.setServletPath("/");
        res = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
    }

    @Test
    void nullAuthenticationRunsRestOfFilterchain() throws IOException, ServletException {
        underTest = new FilterImpl(() -> null);
        underTest.doFilter(req, res, filterChain);
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void authenticationExceptionStopsFilterchain() throws IOException, ServletException {
        underTest = new FilterImpl(() -> {
            throw new BadCredentialsException("bad");
        });
        underTest.doFilter(req, res, filterChain);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void validAuthenticationIsProcessed() throws IOException, ServletException {
        underTest = new FilterImpl(() -> mock(Authentication.class));
        underTest.doFilter(req, res, filterChain);
        verify(filterChain, never()).doFilter(any(), any());
    }

}
