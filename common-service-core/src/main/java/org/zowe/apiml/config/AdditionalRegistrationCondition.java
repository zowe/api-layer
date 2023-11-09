/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.List;
import java.util.stream.Collectors;

import static org.zowe.apiml.config.AdditionalRegistrationParser.DISCOVERYSERVICEURLS_PATTERN;

@Slf4j
@RequiredArgsConstructor
public class AdditionalRegistrationCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String dcUrls = context.getEnvironment().getProperty("apiml.service.additionalRegistration[0].discoveryServiceUrls");
        List<String> additionalKeys = ((StandardEnvironment) context.getEnvironment()).getSystemEnvironment()
            .entrySet().stream().map(e -> e.getKey().toUpperCase()).filter(key -> DISCOVERYSERVICEURLS_PATTERN.matcher(key).matches())
            .collect(Collectors.toList());
        boolean isAdditionalRegistrationsDetected = dcUrls != null || !additionalKeys.isEmpty();
        log.debug("isAdditionalRegistrationsDetected: {}", isAdditionalRegistrationsDetected);
        return isAdditionalRegistrationsDetected;
    }
}
