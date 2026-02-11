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
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Nonnull;

@Slf4j
public abstract class ApimlOpenTelemetryResourceProvider implements ResourceProvider {

    @Value("${otel.resource.attributes.service.namespace:#{null}}")
    private String serviceNamespace;

    @Value("${apiml.service.hostname:localhost}")
    protected String hostname;

    @Value("${apiml.service.port:10010}")
    protected int port;

    @Value("${otel.resource.attributes.service.name:#{null}}")
    private String serviceName;

    @Value("${apiml.service.apimlId:#{null}}")
    protected String apimlId;

    public @Nonnull Attributes calculateAttributes() {
        var attributesBuilder = Attributes.builder();

        if (StringUtils.isBlank(serviceNamespace)) {
            log.debug("service.namespace is not provided in configuration");
        }

        if (StringUtils.isBlank(serviceName)) {
            var generatedServiceName = generateServiceName();
            attributesBuilder.put("service.name", generatedServiceName);
            log.debug("service.name not provided in configuration, using generated default {}", generatedServiceName);
        }

        attributesBuilder.put(ZosOpenTelemetryAttributes.OTEL_ZOS_INSTANCE_ID, generateInstanceId());

        attributesBuilder.putAll(internalCalculateAttributes());
        return attributesBuilder.build();
    }

    protected abstract @Nonnull Attributes internalCalculateAttributes();

    @Override
    public Resource createResource(@Nonnull ConfigProperties config) {
        var attributesBuilder = Attributes.builder();

        attributesBuilder.putAll(calculateAttributes());
        return Resource.create(attributesBuilder.build());
    }

    private String generateInstanceId() {
        return String.format("%s:gateway:%d", hostname, port);
    }

    protected abstract String generateServiceName();

}
