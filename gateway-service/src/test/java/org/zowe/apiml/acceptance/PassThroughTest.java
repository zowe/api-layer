/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.acceptance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.netflix.zuul.filters.discovery.ServiceRouteMapper;
import org.springframework.cloud.netflix.zuul.filters.discovery.SimpleServiceRouteMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.zowe.apiml.gateway.GatewayApplication;

@SpringBootTest(classes = GatewayApplication.class)
@Import(PassThroughTest.Configuration.class)
public class PassThroughTest {

    @Test
    void test() {
        System.out.print("Tested");
    }

    @TestConfiguration
    public static class Configuration {
        @Bean
        @Primary
        public ServiceRouteMapper serviceRouteMapper() {
            return new SimpleServiceRouteMapper();
        }
    }
}
