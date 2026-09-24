/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.client.configuration;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.zowe.apiml.product.service.ServiceStartupEventHandler;
import tools.jackson.databind.DeserializationFeature;

/**
 * Configuration for Spring Boot components.
 * This is the customization of Jackson deserializer.
 */
@SpringBootConfiguration
public class SpringComponentsConfiguration {

    /**
     * Spring Boot 4 builds the web message converters from a Jackson 3 {@code JsonMapper}, so the
     * customization has to target the Jackson 3 builder. The previous Jackson 2
     * {@code Jackson2ObjectMapperBuilderCustomizer} is still on the classpath (kept for the Jackson 2
     * shim) but is no longer applied to the HTTP converters, which silently dropped this setting and
     * turned the expected 400 into a 404.
     */
    @Bean
    JsonMapperBuilderCustomizer failOnUnknownProperties() {
        return jsonMapperBuilder -> jsonMapperBuilder
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Bean
    ServiceStartupEventHandler serviceStartupEventHandler() {
        return new ServiceStartupEventHandler();
    }

}
