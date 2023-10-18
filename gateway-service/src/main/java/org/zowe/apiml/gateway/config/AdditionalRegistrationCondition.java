/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.web.context.support.StandardServletEnvironment;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class AdditionalRegistrationCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String dcUrls = context.getEnvironment().getProperty("apiml.service.additionalRegistration[0].discoveryServiceUrls");
        List<String> additionalKeys = ((StandardServletEnvironment) context.getEnvironment()).getSystemEnvironment()
            .entrySet().stream().map(e -> e.getKey().toUpperCase()).filter(key -> key.startsWith(AdditionalRegistrationConfig.COMMON_PREFIX))
            .collect(Collectors.toList());
        boolean isAdditionalRegistrationsDetected = dcUrls != null || !additionalKeys.isEmpty();
        log.debug("isAdditionalRegistrationsDetected: {}", isAdditionalRegistrationsDetected);
        return isAdditionalRegistrationsDetected;
    }
}
