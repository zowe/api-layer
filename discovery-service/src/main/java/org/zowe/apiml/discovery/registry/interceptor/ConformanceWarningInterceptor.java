/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.registry.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.exception.MetadataValidationException;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import org.zowe.apiml.registry.model.ServiceInstance;
import org.zowe.apiml.registry.spi.RegistrationInterceptor;
import org.zowe.apiml.util.EurekaUtils;

import java.util.Map;
import java.util.Objects;

import static org.zowe.apiml.constants.EurekaMetadataDefinition.AUTHENTICATION_APPLID;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.AUTHENTICATION_SCHEME;

/**
 * Reports non-conformant service identities and authentication configuration.
 * <p>
 * Warnings only - it never rejects. Services with these defects register successfully today, and failing them on
 * upgrade would take working systems down for a cosmetic reason. Ported verbatim from
 * {@code ApimlInstanceRegistry.validateInstanceInfo} / {@code verifyAuthenticationSchemeConfiguration}.
 * <p>
 * The applid check is here, on registration, rather than where the Gateway builds its routes: routes are rebuilt
 * on every heartbeat, so warning there floods the log with the same line every thirty seconds per service.
 */
@Slf4j
public class ConformanceWarningInterceptor implements RegistrationInterceptor {

    private static final ApimlLogger apimlLog =
        ApimlLogger.of(ConformanceWarningInterceptor.class, YamlMessageServiceInstance.getInstance());

    @Override
    public ServiceInstance intercept(ServiceInstance instance) {
        String instanceId = instance.instanceId();
        String appName = StringUtils.lowerCase(instance.appName());

        try {
            EurekaUtils.validateServiceId(appName);
        } catch (MetadataValidationException e) {
            log.warn("Conformance criteria violation in serviceId or app in instanceId for: '{}'. Cause: {}",
                instanceId, e.getMessage());
        }

        String serviceId = EurekaUtils.getServiceIdFromInstanceId(instanceId);
        try {
            EurekaUtils.validateServiceId(serviceId);
        } catch (MetadataValidationException e) {
            log.warn("Conformance criteria violation in serviceId or instanceId for '{}'. Cause: {}",
                instanceId, e.getMessage());
        }

        // Case-insensitive: the Java enabler upper-cases appName on the wire while the instanceId keeps the
        // configured casing, so a strict comparison would warn about every conformant service.
        if (!Objects.equals(appName, StringUtils.lowerCase(serviceId))) {
            log.warn("Inconsistent service identity: instanceId contains serviceId '{}' but appName='{}'",
                serviceId, appName);
        }

        verifyAuthenticationScheme(instance, appName);
        return instance;
    }

    private void verifyAuthenticationScheme(ServiceInstance instance, String serviceId) {
        Map<String, String> metadata = instance.metadata();
        if (metadata == null) {
            return;
        }
        boolean usesPassTicket = AuthenticationScheme.HTTP_BASIC_PASSTICKET.getScheme()
            .equals(metadata.get(AUTHENTICATION_SCHEME));
        if (usesPassTicket && StringUtils.isEmpty(metadata.get(AUTHENTICATION_APPLID))) {
            apimlLog.log("org.zowe.apiml.discovery.registration.missingApplid", serviceId);
        }
    }

    @Override
    public int order() {
        return 20;
    }

}
