/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml;

import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.config.DiscoveryClientConfig;
import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
class OnboardingEnablerMicronautTest {

    @Test
    void test() {
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext.class);
        ApiMediationServiceConfig apiMediationServiceConfig = ctx.getBean(DiscoveryClientConfig.class);
        assertEquals("micronautdiscoverableclient", apiMediationServiceConfig.getServiceId());
    }
}
