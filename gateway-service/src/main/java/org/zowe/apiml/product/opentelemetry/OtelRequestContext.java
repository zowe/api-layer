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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.auth.AuthenticationScheme;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public final class OtelRequestContext {

    public static final String OTEL_CONTEXT = "otel-context";

    private static final String OK = "OK";
    private static final String ERROR = "ERROR";
    public static final String BASIC_AUTH_TYPE = "BASIC";

    private static final String OTEL_ATTRIBUTE_METHOD = "http.request.method";
    private static final String OTEL_ATTRIBUTE_SCHEME = "url.scheme";
    private static final String OTEL_ATTRIBUTE_PATH = "url.path";
    private static final String OTEL_ATTRIBUTE_RESPONSE_CODE = "service.response_code";
    private static final String OTEL_ATTRIBUTE_SERVICE_ID = "service.id";
    private static final String OTEL_ATTRIBUTE_INSTANCE_ID = "service.instance.id";
    private static final String OTEL_ATTRIBUTE_AUTH_METHOD = "auth.service.auth.method";
    private static final String OTEL_ATTRIBUTE_AUTH_SOURCE_TYPE = "auth.method";
    private static final String OTEL_ATTRIBUTE_AUTH_STATUS = "auth.status";
    private static final String OTEL_ATTRIBUTE_AUTH_ERROR_TYPE = "auth.error.type";
    private static final String OTEL_ATTRIBUTE_AUTH_ERROR_MESSAGE = "auth.error.message";
    private static final String OTEL_ATTRIBUTE_USER_ID = "user.id";
    private static final String OTEL_ATTRIBUTE_DISTRIBUTED_USER_ID = "user.distributed.id";
    private static final String OTEL_ATTRIBUTE_STATUS_CODE = "http.response.status_code";
    private static final String OTEL_ATTRIBUTE_ERROR_TYPE = "error.type";
    private static final String OTEL_ATTRIBUTE_ERROR_MESSAGE = "error.message";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AttributesBuilder attributesBuilder = Attributes.builder();

    // this mart is for other codes to mark a specific call of creating. By marking a specific version could be selected
    private AtomicBoolean marked = new AtomicBoolean(false);

    private OtelRequestContext() {
    }

    public static OtelRequestContext of(ServerWebExchange exchange) {
        return (OtelRequestContext) exchange.getAttributes().computeIfAbsent(OTEL_CONTEXT, key -> new OtelRequestContext());
    }

    public boolean mark() {
        return marked.compareAndSet(false, true);
    }

    public OtelRequestContext put(final String key, final String value) {
        attributesBuilder.put(key, value);
        return this;
    }

    public OtelRequestContext method(HttpMethod httpMethod) {
        return put(OTEL_ATTRIBUTE_METHOD, String.valueOf(httpMethod));
    }

    public OtelRequestContext scheme(String scheme) {
        return put(OTEL_ATTRIBUTE_SCHEME, scheme);
    }

    public OtelRequestContext path(String path) {
        return put(OTEL_ATTRIBUTE_PATH, path);
    }

    public OtelRequestContext responseCode(int status) {
        return put(OTEL_ATTRIBUTE_RESPONSE_CODE, String.valueOf(status));
    }

    public OtelRequestContext statusCode(int status) {
        return put(OTEL_ATTRIBUTE_STATUS_CODE, String.valueOf(status));
    }

    public OtelRequestContext errorType(String errorType) {
        return put(OTEL_ATTRIBUTE_ERROR_TYPE, errorType);
    }

    public OtelRequestContext errorMessage(String errorMessage) {
        return put(OTEL_ATTRIBUTE_ERROR_MESSAGE, errorMessage);
    }

    public OtelRequestContext serviceId(String serviceId) {
        return put(OTEL_ATTRIBUTE_SERVICE_ID, StringUtils.lowerCase(serviceId));
    }

    public OtelRequestContext instanceId(String instanceId) {
        return put(OTEL_ATTRIBUTE_INSTANCE_ID, StringUtils.lowerCase(instanceId));
    }

    public OtelRequestContext authMethod(AuthenticationScheme authenticationScheme) {
        return put(OTEL_ATTRIBUTE_AUTH_METHOD, String.valueOf(authenticationScheme));
    }

    public OtelRequestContext authMethod(String authenticationScheme) {
        return put(OTEL_ATTRIBUTE_AUTH_METHOD, authenticationScheme);
    }

    public OtelRequestContext authenticationFailed() {
        return put(OTEL_ATTRIBUTE_AUTH_STATUS, ERROR);
    }

    public OtelRequestContext authErrorType(String authErrorType) {
        return put(OTEL_ATTRIBUTE_AUTH_ERROR_TYPE, authErrorType);
    }

    public OtelRequestContext authErrorMessage(String authErrorMessage) {
        return put(OTEL_ATTRIBUTE_AUTH_ERROR_MESSAGE, authErrorMessage);
    }

    public OtelRequestContext authenticationSuccess() {
        return put(OTEL_ATTRIBUTE_AUTH_STATUS, OK);
    }

    public OtelRequestContext userId(String userId) {
        return put(OTEL_ATTRIBUTE_USER_ID, StringUtils.upperCase(userId));
    }

    public OtelRequestContext distributedIds(List<String> distributedIds) {
        attributesBuilder.put(OTEL_ATTRIBUTE_DISTRIBUTED_USER_ID, distributedIds.toArray(new String[0]));
        return this;
    }

    public OtelRequestContext authSourceType(String authSourceType) {
        return put(OTEL_ATTRIBUTE_AUTH_SOURCE_TYPE, authSourceType);
    }

    @VisibleForTesting
    ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    @Override
    public String toString() {
        var attributes = attributesBuilder.build();
        try {
            return getObjectMapper().writeValueAsString(attributes.asMap());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize attributes", e);
        }
    }

    protected Logger getOtelLogger() {
        return LoggerFactory.getLogger("org.zowe.apiml.opentelemetry");
    }

    public void issue() {
        var logger = getOtelLogger();
        if (logger.isInfoEnabled()) {
            logger.info(toString());
        }
    }

}
