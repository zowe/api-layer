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

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.web.context.support.StandardServletEnvironment;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdditionalRegistrationConditionTest {

    @Test
    void givenEnvironmentVariables_thenReturnTrue() {
        AdditionalRegistrationCondition crc = new AdditionalRegistrationCondition();
        ConditionContext context = mock(ConditionContext.class);
        StandardServletEnvironment env = mock(StandardServletEnvironment.class);
        Map<String, Object> systemEnv = new HashMap<>();
        systemEnv.put("ZWE_configs_apiml_service_additionalRegistration_0_DISCOVERYSERVICEURLS", "https://localhost:123,https://localhostr:345");
        systemEnv.put("ZWE_configs_apiml_service_additionalRegistration_1_DISCOVERYSERVICEURLS", "https://localhost:555,https://localhostr:666");
        when(env.getSystemEnvironment()).thenReturn(systemEnv);
        when(context.getEnvironment()).thenReturn(env);
        assertTrue(crc.matches(context, null));
    }

    @Test
    void givenPropertyVariables_thenReturnTrue() {
        AdditionalRegistrationCondition crc = new AdditionalRegistrationCondition();
        ConditionContext context = mock(ConditionContext.class);
        StandardServletEnvironment env = mock(StandardServletEnvironment.class);
        when(env.getProperty("apiml.service.additionalRegistration[0].discoveryServiceUrls")).thenReturn("https://localhost:123,https://localhostr:345");
        when(context.getEnvironment()).thenReturn(env);
        assertTrue(crc.matches(context, null));
    }

    @Test
    void givenMissingConfiguration_thenReturnFalse() {
        AdditionalRegistrationCondition crc = new AdditionalRegistrationCondition();
        ConditionContext context = mock(ConditionContext.class);
        StandardServletEnvironment env = mock(StandardServletEnvironment.class);
        when(context.getEnvironment()).thenReturn(env);
        assertFalse(crc.matches(context, null));
    }

}
