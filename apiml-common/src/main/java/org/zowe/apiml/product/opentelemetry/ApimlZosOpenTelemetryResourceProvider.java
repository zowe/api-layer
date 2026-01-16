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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zowe.apiml.product.zos.ZosSystemInformation;

import javax.annotation.Nonnull;

import java.util.Map;

@Component
@RequiredArgsConstructor
//@ConditionalOnExpression -> Expression should depend on ZosSystemInformation.isRunningOnZos()
@Slf4j
public class ApimlZosOpenTelemetryResourceProvider extends ApimlOpenTelemetryResourceProvider {

    private final ZosSystemInformation zosSystemInformation;

    @Value("${otel.resource.attributes.deployment.environment.name}")
    private String environmentName;

    @Value("${otel.resource.attributes.zos.lpar.name}")
    private String lparName;

    @Value("${otel.resource.attributes.zos.lpar.override}")
    private boolean lparOverride;

    @Value("${otel.resource.attributes.service.namespace}")
    private String serviceNamespace;

    @Value("${otel.resource.attributes.service.name}")
    private String serviceName;

    @SuppressWarnings("null")
    @Override
    @Nonnull
    Attributes calculateAttributes() {
        if (!zosSystemInformation.isRunningOnZos()) {
            log.error("OpenTelemetry attributes provider running outside of z/OS");
            return Attributes.empty();
        }
        var attributesBuilder = Attributes.builder();

        var zosAttributes = zosSystemInformation.get();

        if (lparOverride && StringUtils.isNotBlank(lparName)) {
            attributesBuilder.put("mainframe.lpar.name", lparName);
        }

        if (StringUtils.isBlank(serviceNamespace)) {
            attributesBuilder.put("service.namespace", generateServiceNamespace(zosAttributes));
        }

        if (StringUtils.isBlank(serviceName)) {
            attributesBuilder.put("service.name", generateServiceName(zosAttributes));
        }

        return attributesBuilder.build();
    }

    private String generateServiceName(Map<String,Object> zosAttributes) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'generateServiceName'");
    }

    private String generateServiceNamespace(Map<String,Object> zosAttributes) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'generateServiceNamespace'");
    }

}
