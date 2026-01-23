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

import static org.zowe.apiml.product.zos.ZosSystemInformation.*;

@Component
@RequiredArgsConstructor
@ConditionalOnMissingBean(ApimlNonZosOpenTelemetryResourceProvider.class)
@Slf4j
public class ApimlZosOpenTelemetryResourceProvider extends ApimlOpenTelemetryResourceProvider {

    private final ZosSystemInformation zosSystemInformation;

    @Value("${otel.resource.attributes.deployment.environment.name:#{null}}")
    private String environmentName;

    @Value("${otel.resource.attributes.zos.lpar.name:#{null}}")
    private String lparName;

    @Value("${otel.resource.attributes.zos.lpar.override:false}")
    private boolean lparOverride;

    @Value("${otel.resource.attributes.service.namespace:#{null}}")
    private String serviceNamespace;

    @Value("${otel.resource.attributes.service.name:#{null}}")
    private String serviceName;

    @Value("${apiml.service.apimlId:#{null}}")
    private String apimlId;

    @PostConstruct
    void afterPropertiesSet() {
        log.debug("Using ZOS OpenTelemetry resource provider");
    }

    @SuppressWarnings("null")
    @Override
    @Nonnull
    Attributes calculateAttributes() {
        if (!ZosSystemInformation.isRunningOnZos()) {
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

        Optional.ofNullable(zosAttributes.get(ZOS_JOB_ID)).map(String::valueOf)
            .ifPresent(zosJobId -> attributesBuilder.put("zos.jobid", zosJobId));

        Optional.ofNullable(zosAttributes.get(ZOS_JOB_NAME)).map(String::valueOf)
            .ifPresent(zosJobName -> attributesBuilder.put("zos.jobname", zosJobName));

        Optional.ofNullable(zosAttributes.get(ZOS_USER_ID)).map(String::valueOf)
            .ifPresent(zosUserId -> attributesBuilder.put("zos.userid", zosUserId));

        Optional.ofNullable(zosAttributes.get(ZOS_PID)).map(String::valueOf)
            .ifPresent(zosPid -> attributesBuilder.put("zos.pid", zosPid));

        Optional.ofNullable(zosAttributes.get(ZOS_SYSNAME)).map(String::valueOf)
            .ifPresent(zosSysname -> attributesBuilder.put("zos.sysname", zosSysname));

        Optional.ofNullable(zosAttributes.get(ZOS_SYSCLONE)).map(String::valueOf)
            .ifPresent(zosSysclone -> attributesBuilder.put("zos.sysclone", zosSysclone));

        Optional.ofNullable(zosAttributes.get(ZOS_SYSPLEX)).map(String::valueOf)
            .ifPresent(zosSysplex -> attributesBuilder.put("zos.sysplex", zosSysplex));

        return attributesBuilder.build();
    }

    private String generateServiceName(Map<String,Object> zosAttributes) {
        return "service_name";
    }

    private String generateServiceNamespace(Map<String,Object> zosAttributes) {
        return "service_namespace";
    }

}
