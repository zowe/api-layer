/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.util.service;

import lombok.Getter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.function.Predicate;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Class is using for verification request in VirtualService. If test call VerifyServlet in there, request data are
 * stored to next verification (see asserts). In this class are copied values from HttpServletRequest. Not all, but
 * the most important part. List of stored values could be extended.
 *
 * This class is design as singleton, same for virtual services in the test
 */
public class RequestVerifier {

    private static RequestVerifier instance;

    private final Map<VirtualService, List<HttpRequestCopy>> requests = new HashMap<>();

    private RequestVerifier() {
    }

    public static RequestVerifier getInstance() {
        if (instance != null) return instance;
        synchronized (RequestVerifier.class) {
            if (instance == null) {
                instance = new RequestVerifier();
            }
            return instance;
        }
    }

    public void clear() {
        synchronized (requests) {
            requests.clear();
        }
    }

    public void clear(VirtualService virtualService) {
        synchronized (requests) {
            requests.remove(virtualService);
        }
    }

    public void add(VirtualService virtualService, HttpServletRequest httpServletRequest) {
        synchronized (requests) {
            HttpRequestCopy copy = new HttpRequestCopy(httpServletRequest);

            List<HttpRequestCopy> list = requests.computeIfAbsent(virtualService, x -> new LinkedList<>());
            list.add(copy);
        }
    }

    public void existAndClean(VirtualService virtualService, Predicate<HttpRequestCopy> verify) {
        synchronized (requests) {
            List<HttpRequestCopy> list = requests.get(virtualService);
            assertNotNull("No request exists", list);
            for (Iterator<HttpRequestCopy> i = list.iterator(); i.hasNext(); ) {
                HttpRequestCopy req = i.next();
                if (verify.test(req)) {
                    i.remove();
                    return;
                }
            }

            fail(list.isEmpty() ? "No request exists" : "Request was not found, but cache contains " + list.size() + " requests");
        }
    }

    @Getter
    public static class HttpRequestCopy {

        private final String authType;
        private final Cookie[] cookies;
        private final String pathInfo;
        private final String contextPath;
        private final String queryString;
        private final String requestURI;
        private final StringBuffer requestURL;

        private final Map<String, List<String>> headersData = new HashMap<>();

        private HttpRequestCopy(HttpServletRequest request) {
            this.authType = request.getAuthType();
            this.cookies = request.getCookies();
            this.pathInfo = request.getPathInfo();
            this.contextPath = request.getContextPath();
            this.queryString = request.getQueryString();
            this.requestURI = request.getRequestURI();
            this.requestURL = request.getRequestURL();

            for (final Enumeration<String> e = request.getHeaderNames(); e.hasMoreElements(); ) {
                final String headerName = e.nextElement();

                final List<String> headers = new LinkedList<>();
                for (final Enumeration<String> e2 = request.getHeaders(headerName); e2.hasMoreElements(); ) {
                    headers.add(e2.nextElement());
                }
                headersData.put(headerName.toLowerCase(), headers);
            }
        }

        public String getHeader(String name) {
            final List<String> list = headersData.get(name.toLowerCase());
            if ((list == null) || list.isEmpty()) return null;
            return list.get(0);
        }

        public Enumeration<String> getHeaders(String name) {
            List list = headersData.get(name);
            if (list == null) list = Collections.emptyList();
            return Collections.enumeration(list);
        }

    }

}
