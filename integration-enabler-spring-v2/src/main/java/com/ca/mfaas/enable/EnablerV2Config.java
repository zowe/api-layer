/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.enable;

import com.ca.mfaas.enable.conditions.ConditionalOnMissingProperty;
import com.ca.mfaas.enable.model.ApiPropertiesContainer;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@ConditionalOnProperty(prefix = "eureka.instance.metadata-map.mfaas.discovery", value = "enableApiDoc", havingValue = "true", matchIfMissing = true)
@Configuration
@EnableSwagger2
public class EnablerV2Config {

    @ConditionalOnMissingProperty("eureka.instance.metadata-map.mfaas.api-info.swagger.location")
    @Bean
    public EnablerV2SpringFoxConfig enablerV2SpringFoxConfig(ApiPropertiesContainer apiPropertiesContainer,
                                                             DefaultListableBeanFactory beanFactory) {
        return new EnablerV2SpringFoxConfig(apiPropertiesContainer, beanFactory);
    }

}
