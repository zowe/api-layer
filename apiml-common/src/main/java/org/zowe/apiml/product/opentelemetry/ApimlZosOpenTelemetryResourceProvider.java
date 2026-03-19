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
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.zowe.apiml.product.zos.ZosSystemInformation;

import javax.annotation.Nonnull;

import java.util.Optional;

import static org.zowe.apiml.product.zos.ZosSystemInformation.ZOS_ENVIRON;
import static org.zowe.apiml.product.zos.ZosSystemInformation.ZOS_JOB_NAME;
import static org.zowe.apiml.product.zos.ZosSystemInformation.ZOS_SMF_ID;
import static org.zowe.apiml.product.zos.ZosSystemInformation.ZOS_SYSNAME;
import static org.zowe.apiml.product.zos.ZosSystemInformation.ZOS_SYSPLEX;
import static org.zowe.apiml.product.zos.ZosSystemInformation.ZOS_USER_ID;

@Component
@RequiredArgsConstructor
@Profile("zos")
@Slf4j
public class ApimlZosOpenTelemetryResourceProvider extends ApimlOpenTelemetryResourceProvider {

    private final ZosSystemInformation zosSystemInformation;

    @Value("${otel.resource.attributes.deployment.environment.name:#{null}}")
    private String environmentName;

    @Value("${otel.resource.attributes.zos.sysplex.name:#{null}}")
    private String sysplexName;

    @Value("${otel.resource.attributes.mainframe.lpar.name:#{null}}")
    private String lparName;

    @Value("${otel.resource.attributes.zos.smf.id:#{null}}")
    private String smfId;

    @PostConstruct
    void afterPropertiesSet() {
        log.debug("Using ZOS OpenTelemetry resource provider");
    }

    private void attribute(AttributesBuilder attributesBuilder, @Nonnull String openTelemetryAttribute, String zosAttribute) {
        var zosAttributes = zosSystemInformation.get();
        var zosValue = zosAttributes.get(zosAttribute);
        if (zosValue != null && StringUtils.isNotBlank(zosValue.toString())) {
            log.debug(openTelemetryAttribute + " not provided in configuration, using z/OS obtained {}", zosValue);
            attributesBuilder.put(openTelemetryAttribute, zosValue.toString());
        } else {
            log.debug(openTelemetryAttribute + " not provided in configuration. Could not determine it from system");
        }
    }

    @SuppressWarnings("null")
    @Override
    @Nonnull
    protected Attributes internalCalculateAttributes() {
        var attributesBuilder = Attributes.builder();

        var zosAttributes = zosSystemInformation.get();

        if (StringUtils.isBlank(environmentName)) {
            attribute(attributesBuilder, "deployment.environment.name", ZOS_ENVIRON);
        }

        if (StringUtils.isBlank(sysplexName)) {
            attribute(attributesBuilder, "zos.sysplex.name", ZOS_SYSPLEX);
        }

        if (StringUtils.isBlank(lparName)) {
            attribute(attributesBuilder, "mainframe.lpar.name", ZOS_SYSNAME);
        }

        if (StringUtils.isBlank(smfId)) {
            attribute(attributesBuilder, "zos.smf.id", ZOS_SMF_ID);
        }

        Optional.ofNullable(zosAttributes.get(ZOS_JOB_NAME))
            .map(String::valueOf)
            .filter(StringUtils::isNotBlank)
            .ifPresent(zosJobName -> attributesBuilder.put(ZosOpenTelemetryAttributes.OTEL_ZOS_JOBNAME, zosJobName));

        Optional.ofNullable(zosAttributes.get(ZOS_USER_ID))
            .map(String::valueOf)
            .filter(StringUtils::isNotBlank)
            .ifPresent(zosUserId -> attributesBuilder.put(ZosOpenTelemetryAttributes.OTEL_ZOS_USERID, zosUserId));

        return attributesBuilder.build();
    }

    @Override
    protected String generateServiceName() {
        var zosAttributes = zosSystemInformation.get();
        var systemName = StringUtils.isBlank(apimlId) ? zosAttributes.get(ZOS_SYSPLEX) : apimlId;
        return "apiml:" + systemName + ":" + port;
    }

}
