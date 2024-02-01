/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.springframework.boot.actuate.trace.http;

import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HttpTrace {
    private final Instant timestamp;
    private volatile Principal principal;
    private volatile Session session;
    private final Request request;
    private volatile Response response;
    private volatile Long timeTaken;
    private final long startNanoTime;

    public HttpTrace(Request request, Response response, Instant timestamp, Principal principal, Session session, Long timeTaken) {
        this.request = request;
        this.response = response;
        this.timestamp = timestamp;
        this.principal = principal;
        this.session = session;
        this.timeTaken = timeTaken;
        this.startNanoTime = 0L;
    }

    HttpTrace(TraceableRequest request) {
        this.request = new Request(request);
        this.timestamp = Instant.now();
        this.startNanoTime = System.nanoTime();
    }

    public Instant getTimestamp() {
        return this.timestamp;
    }

    void setPrincipal(java.security.Principal principal) {
        if (principal != null) {
            this.principal = new Principal(principal.getName());
        }

    }

    public Principal getPrincipal() {
        return this.principal;
    }

    public Session getSession() {
        return this.session;
    }

    void setSessionId(String sessionId) {
        if (StringUtils.hasText(sessionId)) {
            this.session = new Session(sessionId);
        }

    }

    public Request getRequest() {
        return this.request;
    }

    public Response getResponse() {
        return this.response;
    }

    void setResponse(Response response) {
        this.response = response;
    }

    public Long getTimeTaken() {
        return this.timeTaken;
    }

    void setTimeTaken(long timeTaken) {
        this.timeTaken = timeTaken;
    }

    long getStartNanoTime() {
        return this.startNanoTime;
    }

    public static final class Principal {
        private final String name;

        public Principal(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

    public static final class Session {
        private final String id;

        public Session(String id) {
            this.id = id;
        }

        public String getId() {
            return this.id;
        }
    }

    public static final class Response {
        private final int status;
        private final Map<String, List<String>> headers;

        Response(TraceableResponse response) {
            this(response.getStatus(), response.getHeaders());
        }

        public Response(int status, Map<String, List<String>> headers) {
            this.status = status;
            this.headers = new LinkedHashMap(headers);
        }

        public int getStatus() {
            return this.status;
        }

        public Map<String, List<String>> getHeaders() {
            return this.headers;
        }
    }

    public static final class Request {
        private final String method;
        private final URI uri;
        private final Map<String, List<String>> headers;
        private final String remoteAddress;

        private Request(TraceableRequest request) {
            this(request.getMethod(), request.getUri(), request.getHeaders(), request.getRemoteAddress());
        }

        public Request(String method, URI uri, Map<String, List<String>> headers, String remoteAddress) {
            this.method = method;
            this.uri = uri;
            this.headers = new LinkedHashMap(headers);
            this.remoteAddress = remoteAddress;
        }

        public String getMethod() {
            return this.method;
        }

        public URI getUri() {
            return this.uri;
        }

        public Map<String, List<String>> getHeaders() {
            return this.headers;
        }

        public String getRemoteAddress() {
            return this.remoteAddress;
        }
    }
}
