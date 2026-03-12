/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.opentelemetry;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.auth.AuthenticationScheme;

import java.util.List;

@Slf4j
public final class RoutingContext {

    private static final String OTEL_CONTEXT = "otel-context";

    private static final String OK = "OK";
    private static final String FAILED = "FAILED";

    private static final String OTEL_ATTRIBUTE_METHOD = "http.request.method";
    private static final String OTEL_ATTRIBUTE_SCHEME = "url.scheme";
    private static final String OTEL_ATTRIBUTE_PATH = "url.path";
    private static final String OTEL_ATTRIBUTE_RESPONSE_CODE = "service.response_code";
    private static final String OTEL_ATTRIBUTE_SERVICE_ID = "service.id";
    private static final String OTEL_ATTRIBUTE_INSTANCE_ID = "service.instance.id";
    private static final String OTEL_ATTRIBUTE_AUTH_METHOD = "auth.method";
    private static final String OTEL_ATTRIBUTE_AUTH_STATUS = "auth.status";
    private static final String OTEL_ATTRIBUTE_USER_ID = "user.id";
    private static final String OTEL_ATTRIBUTE_DISTRIBUTED_USER_ID = "user.distributed.id";

    private AttributesBuilder attributesBuilder = Attributes.builder();;

    private RoutingContext() {
        // set defaults
        authMethod(AuthenticationScheme.BYPASS);
    }

    public static RoutingContext of(ServerWebExchange exchange) {
        return (RoutingContext) exchange.getAttributes().computeIfAbsent(OTEL_CONTEXT, key -> new RoutingContext());
    }

    public RoutingContext put(final String key, final String value) {
        attributesBuilder.put(key, value);
        return this;
    }

    public RoutingContext method(HttpMethod httpMethod) {
        return put(OTEL_ATTRIBUTE_METHOD, String.valueOf(httpMethod));
    }

    public RoutingContext scheme(String scheme) {
        return put(OTEL_ATTRIBUTE_SCHEME, scheme);
    }

    public RoutingContext path(String path) {
        return put(OTEL_ATTRIBUTE_PATH, path);
    }

    public RoutingContext responseCode(int status) {
        return put(OTEL_ATTRIBUTE_RESPONSE_CODE, String.valueOf(status));
    }

    public RoutingContext serviceId(String serviceId) {
        return put(OTEL_ATTRIBUTE_SERVICE_ID, serviceId);
    }

    public RoutingContext instanceId(String instanceId) {
        return put(OTEL_ATTRIBUTE_INSTANCE_ID, instanceId);
    }

    public RoutingContext authMethod(AuthenticationScheme authenticationScheme) {
        return put(OTEL_ATTRIBUTE_AUTH_METHOD, String.valueOf(authenticationScheme));
    }

    public RoutingContext authenticationFailed() {
        return put(OTEL_ATTRIBUTE_AUTH_STATUS, FAILED);
    }

    public RoutingContext authenticationSuccess() {
        return put(OTEL_ATTRIBUTE_AUTH_STATUS, OK);
    }

    public RoutingContext userId(String userId) {
        return put(OTEL_ATTRIBUTE_USER_ID, userId);
    }

    public RoutingContext distributedIds(List<String> distributedIds) {
        return put(OTEL_ATTRIBUTE_DISTRIBUTED_USER_ID, toString(distributedIds));
    }

    private static String toString(List<String> values) {
        if (values != null && !values.isEmpty()) {
            if (values.size() == 1) {
                return values.get(0);
            }

            // TODO: use escaping characters?
            return String.join(",", values);
        }
        return null;
    }

    public void issue() {
        var logger = LoggerFactory.getLogger("org.zowe.apiml.opentelemetry");
        logger.info(attributesBuilder.build().asMap().toString());
    }

}
