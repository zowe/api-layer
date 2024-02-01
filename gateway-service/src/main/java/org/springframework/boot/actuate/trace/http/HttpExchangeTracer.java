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

import java.net.URI;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class HttpExchangeTracer {
    private final Set<Include> includes;

    public HttpExchangeTracer(Set<Include> includes) {
        this.includes = includes;
    }

    public final HttpTrace receivedRequest(TraceableRequest request) {
        return new HttpTrace(new FilteredTraceableRequest(request));
    }

    public final void sendingResponse(HttpTrace trace, TraceableResponse response, Supplier<Principal> principal, Supplier<String> sessionId) {
        this.setIfIncluded(Include.TIME_TAKEN, () -> {
            return this.calculateTimeTaken(trace);
        }, trace::setTimeTaken);
        this.setIfIncluded(Include.SESSION_ID, sessionId, trace::setSessionId);
        this.setIfIncluded(Include.PRINCIPAL, principal, trace::setPrincipal);
        trace.setResponse(new HttpTrace.Response(new FilteredTraceableResponse(response)));
    }

    protected void postProcessRequestHeaders(Map<String, List<String>> headers) {
    }

    private <T> T getIfIncluded(Include include, Supplier<T> valueSupplier) {
        return this.includes.contains(include) ? valueSupplier.get() : null;
    }

    private <T> void setIfIncluded(Include include, Supplier<T> supplier, Consumer<T> consumer) {
        if (this.includes.contains(include)) {
            consumer.accept(supplier.get());
        }

    }

    private Map<String, List<String>> getHeadersIfIncluded(Include include, Supplier<Map<String, List<String>>> headersSupplier, Predicate<String> headerPredicate) {
        return (Map)(!this.includes.contains(include) ? new LinkedHashMap() : (Map)((Map<String, List<String>>)headersSupplier.get()).entrySet().stream().filter(entry -> {
            return headerPredicate.test(entry.getKey());
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    private long calculateTimeTaken(HttpTrace trace) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - trace.getStartNanoTime());
    }

    private final class FilteredTraceableResponse implements TraceableResponse {
        private final TraceableResponse delegate;

        private FilteredTraceableResponse(TraceableResponse delegate) {
            this.delegate = delegate;
        }

        public int getStatus() {
            return this.delegate.getStatus();
        }

        public Map<String, List<String>> getHeaders() {
            return HttpExchangeTracer.this.getHeadersIfIncluded(Include.RESPONSE_HEADERS, this.delegate::getHeaders, this::includedHeader);
        }

        private boolean includedHeader(String name) {
            return name.equalsIgnoreCase("Set-Cookie") ? HttpExchangeTracer.this.includes.contains(Include.COOKIE_HEADERS) : true;
        }
    }

    private final class FilteredTraceableRequest implements TraceableRequest {
        private final TraceableRequest delegate;

        private FilteredTraceableRequest(TraceableRequest delegate) {
            this.delegate = delegate;
        }

        public String getMethod() {
            return this.delegate.getMethod();
        }

        public URI getUri() {
            return this.delegate.getUri();
        }

        public Map<String, List<String>> getHeaders() {
            Map<String, List<String>> headers = HttpExchangeTracer.this.getHeadersIfIncluded(Include.REQUEST_HEADERS, this.delegate::getHeaders, this::includedHeader);
            HttpExchangeTracer.this.postProcessRequestHeaders(headers);
            return headers;
        }

        private boolean includedHeader(String name) {
            if (name.equalsIgnoreCase("Cookie")) {
                return HttpExchangeTracer.this.includes.contains(Include.COOKIE_HEADERS);
            } else {
                return name.equalsIgnoreCase("Authorization") ? HttpExchangeTracer.this.includes.contains(Include.AUTHORIZATION_HEADER) : true;
            }
        }

        public String getRemoteAddress() {
            HttpExchangeTracer var10000 = HttpExchangeTracer.this;
            Include var10001 = Include.REMOTE_ADDRESS;
            TraceableRequest var10002 = this.delegate;
            var10002.getClass();
            return (String)var10000.getIfIncluded(var10001, var10002::getRemoteAddress);
        }
    }
}
