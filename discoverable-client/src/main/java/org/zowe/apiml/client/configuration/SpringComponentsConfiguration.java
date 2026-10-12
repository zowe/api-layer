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
