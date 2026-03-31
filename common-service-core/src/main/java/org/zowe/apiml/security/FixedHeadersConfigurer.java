/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.header.HeaderWriter;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.util.OnCommittedResponseWrapper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

@RequiredArgsConstructor
public class FixedHeadersConfigurer<H extends HttpSecurityBuilder<H>> extends HeadersConfigurer<H> {

    @Delegate(excludes = Configure.class)
    protected final HeadersConfigurer<H> original;

    public static <T extends HttpSecurityBuilder<T>> HttpSecurity fix(HttpSecurity httpSecurity) throws Exception {
        // remove the invalid configured
        HeadersConfigurer<T> originalConfigurer = httpSecurity.removeConfigurer(HeadersConfigurer.class);

        // add back the fixed version
        httpSecurity.<FixedHeadersConfigurer>apply(new FixedHeadersConfigurer<>(originalConfigurer));

        return httpSecurity;
    }

    @Override
    public void configure(H http) {
        HeaderWriterFilter headersFilter = createHeaderWriterFilterFixed();
        http.addFilter(headersFilter);
    }

    private HeaderWriterFilter createHeaderWriterFilterFixed() {
        List<HeaderWriter> writers;
        try {
            // to do not duplicate code, rather call the original private method
            Method getHeaderWriters = HeadersConfigurer.class.getDeclaredMethod("getHeaderWriters");
            getHeaderWriters.setAccessible(true);
            writers = (List<HeaderWriter>) getHeaderWriters.invoke(original);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException |
                 InvocationTargetException e) {
            throw new IllegalStateException("The implementation was changed", e);
        }

        if (writers.isEmpty()) {
            throw new IllegalStateException(
                "Headers security is enabled, but no headers will be added. Either add headers or disable headers security");
        }
        HeaderWriterFilter headersFilter = new FixedHeaderWriterFilter(writers);
        headersFilter = postProcess(headersFilter);
        return headersFilter;
    }

    interface Configure<H extends HttpSecurityBuilder<H>> {

        void configure(H http);

    }

    abstract static class FixedOnCommittedResponseWrapper extends OnCommittedResponseWrapper {

        FixedOnCommittedResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void addHeader(String name, String value) {
            checkContentLengthHeader(name, value);
            super.addHeader(name, value);
        }

        @Override
        public void addIntHeader(String name, int value) {
            checkContentLengthHeader(name, value);
            super.addIntHeader(name, value);
        }

        @Override
        public void setHeader(String name, String value) {
            checkContentLengthHeader(name, value);
            super.setHeader(name, value);
        }

        @Override
        public void setIntHeader(String name, int value) {
            checkContentLengthHeader(name, value);
            super.setIntHeader(name, value);
        }

        private void checkContentLengthHeader(String name, int value) {
            if ("Content-Length".equalsIgnoreCase(name)) {
                setContentLength(value);
            }
        }

        private void checkContentLengthHeader(String name, String value) {
            if ("Content-Length".equalsIgnoreCase(name)) {
                setContentLength(Integer.parseInt(value));
            }
        }

    }

    static class FixedHeaderWriterFilter extends HeaderWriterFilter {

        private final List<HeaderWriter> headerWriters;

        private boolean shouldWriteHeadersEagerly = false;

        public FixedHeaderWriterFilter(List<HeaderWriter> headerWriters) {
            super(headerWriters);
            this.headerWriters = headerWriters;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
            if (this.shouldWriteHeadersEagerly) {
                doHeadersBeforeCopy(request, response, filterChain);
            }
            else {
                doHeadersAfterFixed(request, response, filterChain);
            }
        }

        private void doHeadersBeforeCopy(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
            writeHeadersCopy(request, response);
            filterChain.doFilter(request, response);
        }

        private void doHeadersAfterFixed(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
            FixedHeaderWriterResponse headerWriterResponse = new FixedHeaderWriterResponse(request, response);
            FixedHeaderWriterRequest headerWriterRequest = new FixedHeaderWriterRequest(request, headerWriterResponse);
            try {
                filterChain.doFilter(headerWriterRequest, headerWriterResponse);
            }
            finally {
                headerWriterResponse.writeHeaders();
            }
        }

        void writeHeadersCopy(HttpServletRequest request, HttpServletResponse response) {
            for (HeaderWriter writer : this.headerWriters) {
                writer.writeHeaders(request, response);
            }
        }

        class FixedHeaderWriterResponse extends FixedOnCommittedResponseWrapper {

            private final HttpServletRequest request;

            FixedHeaderWriterResponse(HttpServletRequest request, HttpServletResponse response) {
                super(response);
                this.request = request;
            }

            @Override
            protected void onResponseCommitted() {
                writeHeaders();
                this.disableOnResponseCommitted();
            }

            protected void writeHeaders() {
                if (isDisableOnResponseCommitted()) {
                    return;
                }
                FixedHeaderWriterFilter.this.writeHeadersCopy(this.request, getHttpResponse());
            }

            private HttpServletResponse getHttpResponse() {
                return (HttpServletResponse) getResponse();
            }

        }

        static class FixedHeaderWriterRequest extends HttpServletRequestWrapper {

            private final FixedHeaderWriterResponse response;

            FixedHeaderWriterRequest(HttpServletRequest request, FixedHeaderWriterResponse response) {
                super(request);
                this.response = response;
            }

            @Override
            public RequestDispatcher getRequestDispatcher(String path) {
                return new FixedHeaderWriterFilter.FixedHeaderWriterRequestDispatcher(super.getRequestDispatcher(path), this.response);
            }

        }

        static class FixedHeaderWriterRequestDispatcher implements RequestDispatcher {

            private final RequestDispatcher delegate;

            private final FixedHeaderWriterResponse response;

            FixedHeaderWriterRequestDispatcher(RequestDispatcher delegate, FixedHeaderWriterResponse response) {
                this.delegate = delegate;
                this.response = response;
            }

            @Override
            public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException {
                this.delegate.forward(request, response);
            }

            @Override
            public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException {
                this.response.onResponseCommitted();
                this.delegate.include(request, response);
            }

        }

    }

}
