/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerAddressPropertiesUpdaterTest {

    private ApplicationContextRunner createContextRunner(String type) {
        return new ApplicationContextRunner()
            .withBean(ServerAddressPropertiesUpdater.class)
            .withInitializer(context -> {
                context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("input", Map.of(
                    "server.address", "127.0.0.1,192.168.0.1,10.0.0.1",
                    "server.port", "8000",
                    "spring.main.web-application-type", type
                )));
                new ServerAddressPropertiesUpdater().postProcessEnvironment(
                    context.getEnvironment(), new SpringApplication()
                );
            });
    }

    @ParameterizedTest(name = "givePortConfiguration_whenInitialize_thenCustomizerAreCreated({0}, {1})")
    @CsvSource({
        "servlet,org.zowe.apiml.product.config.ServerAddressPropertiesUpdater$AdditionalConnectorServlet",
        "reactive,org.zowe.apiml.product.config.ServerAddressPropertiesUpdater$AdditionalConnectorReactive"
    })
    void givePortConfiguration_whenInitialize_thenCustomizerAreCreated(String type, Class<?> customizerClass) {
        createContextRunner(type).run(context -> {
            var customizers = context.getBeansOfType(customizerClass);
            assertEquals(2, customizers.size());
            assertEquals("127.0.0.1", context.getEnvironment().getProperty("server.address"));
            assertEquals("192.168.0.1,10.0.0.1", context.getEnvironment().getProperty("server.address.additional"));

            assertEquals(8000, ReflectionTestUtils.getField(customizers.get("tomcatAdditionalConnector-8000-1"), "port"));
            assertEquals("192.168.0.1", ReflectionTestUtils.getField(customizers.get("tomcatAdditionalConnector-8000-1"), "address"));

            assertEquals(8000, ReflectionTestUtils.getField(customizers.get("tomcatAdditionalConnector-8000-2"), "port"));
            assertEquals("10.0.0.1", ReflectionTestUtils.getField(customizers.get("tomcatAdditionalConnector-8000-2"), "address"));
        });
    }

}
