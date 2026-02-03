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
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.zowe.apiml.product.zos.ZosSystemInformation;

import javax.annotation.Nonnull;

import java.util.Map;
import java.util.Optional;

import static org.zowe.apiml.product.zos.ZosSystemInformation.ZOS_JOB_ID;
import static org.zowe.apiml.product.zos.ZosSystemInformation.ZOS_JOB_NAME;
import static org.zowe.apiml.product.zos.ZosSystemInformation.ZOS_SYSPLEX;
import static org.zowe.apiml.product.zos.ZosSystemInformation.ZOS_USER_ID;

@Component
@RequiredArgsConstructor
@ConditionalOnMissingBean(ApimlNonZosOpenTelemetryResourceProvider.class)
@Slf4j
public class ApimlZosOpenTelemetryResourceProvider extends ApimlOpenTelemetryResourceProvider {

    private final ZosSystemInformation zosSystemInformation;

    @Value("${otel.resource.attributes.deployment.environment.name:#{null}}")
    private String environmentName;

    @Value("${otel.resource.attributes.service.namespace:#{null}}")
    private String serviceNamespace;

    @Value("${otel.resource.attributes.service.name:#{null}}")
    private String serviceName;

    @Value("${apiml.service.apimlId:#{null}}")
    private String apimlId;

    @Value("${apiml.service.port:10010}")
    private int port;

    @PostConstruct
    void afterPropertiesSet() {
        log.debug("Using ZOS OpenTelemetry resource provider");
    }

    @SuppressWarnings("null")
    @Override
    @Nonnull
    public Attributes calculateAttributes() {
        var attributesBuilder = Attributes.builder();

        var zosAttributes = zosSystemInformation.get();

        if (StringUtils.isBlank(serviceNamespace)) {
            var generatedDefaultNamespace = generateServiceNamespace(zosAttributes);
            attributesBuilder.put("service.namespace", generatedDefaultNamespace);
            log.debug("service.namespace not provided in configuration, using generated default {}", generatedDefaultNamespace);
        }

        if (StringUtils.isBlank(serviceName)) {
            var generatedServiceName = generateServiceName(zosAttributes);
            attributesBuilder.put("service.name", generatedServiceName);
            log.debug("service.name not provided in configuration, using generated default {}", generatedServiceName);
        }

        Optional.ofNullable(zosAttributes.get(ZOS_JOB_ID)).map(String::valueOf)
            .ifPresent(zosJobId -> attributesBuilder.put(ZosAttributes.ZOS_JOBID, zosJobId));

        Optional.ofNullable(zosAttributes.get(ZOS_JOB_NAME)).map(String::valueOf)
            .ifPresent(zosJobName -> attributesBuilder.put(ZosAttributes.ZOS_JOBNAME, zosJobName));

        Optional.ofNullable(zosAttributes.get(ZOS_USER_ID)).map(String::valueOf)
            .ifPresent(zosUserId -> attributesBuilder.put(ZosAttributes.ZOS_USERID, zosUserId));

        return attributesBuilder.build();
    }

    private String generateServiceName(Map<String,Object> zosAttributes) {
        var systemName = StringUtils.isBlank(apimlId) ? zosAttributes.get(ZOS_SYSPLEX) : apimlId;

        return systemName + ":" + port;
    }

    private String generateServiceNamespace(Map<String,Object> zosAttributes) {
        return "apiml:" + generateServiceName(zosAttributes);
    }

}
