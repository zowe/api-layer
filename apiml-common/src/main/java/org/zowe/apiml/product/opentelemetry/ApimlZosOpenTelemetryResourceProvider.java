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

import java.util.Optional;

import static org.zowe.apiml.product.zos.ZosSystemInformation.ZOS_ENVIRON;
import static org.zowe.apiml.product.zos.ZosSystemInformation.ZOS_JOB_ID;
import static org.zowe.apiml.product.zos.ZosSystemInformation.ZOS_JOB_NAME;
import static org.zowe.apiml.product.zos.ZosSystemInformation.ZOS_SMF_ID;
import static org.zowe.apiml.product.zos.ZosSystemInformation.ZOS_SYSNAME;
import static org.zowe.apiml.product.zos.ZosSystemInformation.ZOS_SYSPLEX;
import static org.zowe.apiml.product.zos.ZosSystemInformation.ZOS_USER_ID;
import static org.zowe.apiml.product.zos.ZosSystemInformation.ZOS_VERSION;

@Component
@RequiredArgsConstructor
@ConditionalOnMissingBean(ApimlNonZosOpenTelemetryResourceProvider.class)
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

    @SuppressWarnings("null")
    @Override
    @Nonnull
    protected Attributes internalCalculateAttributes() {
        var attributesBuilder = Attributes.builder();

        var zosAttributes = zosSystemInformation.get();

        if (StringUtils.isBlank(environmentName)) {
            var environmentName = zosAttributes.get(ZOS_ENVIRON);
            if (environmentName != null && StringUtils.isNotBlank(environmentName.toString())) {
                log.debug("deployment.environment.name not provided in configuration, using z/OS obtained {}", environmentName);
                attributesBuilder.put("deployment.environment.name", environmentName.toString());
            } else {
                log.debug("deployment.environment.name not provided in configuration. Could not determine name from system");
            }
        }

        if (StringUtils.isBlank(sysplexName)) {
            var sysplexName = zosAttributes.get(ZOS_SYSPLEX);
            if (sysplexName != null && StringUtils.isNotBlank(sysplexName.toString())) {
                log.debug("zos.sysplex.name not provided in configuration, using system-obtained {}", sysplexName);
                attributesBuilder.put("zos.sysplex.name", sysplexName.toString());
            } else {
                log.debug("zos.sysplex.name not provided in configuration. Could not determine name from system");
            }
        }

        if (StringUtils.isBlank(lparName)) {
            var lparName = zosAttributes.get(ZOS_SYSNAME);
            if (lparName != null && StringUtils.isNotBlank(lparName.toString())) {
                log.debug("mainframe.lpar.name not provided in configuration, using system-obtained {}", lparName);
                attributesBuilder.put("mainframe.lpar.name", lparName.toString());
            } else {
                log.debug("mainframe.lpar.name not provided in configuration. Could not determine name from system");
            }
        }

        if (StringUtils.isBlank(smfId)) {
            var smfId = zosAttributes.get(ZOS_SMF_ID);
            if (smfId != null && StringUtils.isNotBlank(smfId.toString())) {
                log.debug("zos.smf.id not provided in configuration, using system-obtained {}", smfId);
                attributesBuilder.put("zos.smf.id", smfId.toString());
            } else {
                log.debug("zos.smf.id not provided in configuration. Could not determine ID from system");
            }
        }

        Optional.ofNullable(zosAttributes.get(ZOS_VERSION))
            .map(String::valueOf)
            .filter(StringUtils::isNotBlank)
            .ifPresent(zosVersion -> attributesBuilder.put(ZosOpenTelemetryAttributes.OTEL_OS_VERSION, zosVersion));

        Optional.ofNullable(zosAttributes.get(ZOS_JOB_ID))
            .map(String::valueOf)
            .filter(StringUtils::isNotBlank)
            .ifPresent(zosJobId -> attributesBuilder.put(ZosOpenTelemetryAttributes.OTEL_ZOS_JOBID, zosJobId));

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
        return systemName + ":" + port;
    }

}
