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

import com.ca.mfaas.enable.model.ApiPropertiesContainer;
import com.ca.mfaas.product.conditions.ConditionalOnMissingProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@ConditionalOnProperty(prefix = "eureka.instance.metadata-map.mfaas.discovery", value = "enableApiDoc", havingValue = "true", matchIfMissing = true)
@Configuration
public class EnablerV2Config {

    @ConditionalOnMissingProperty("eureka.instance.metadata-map.mfaas.api-info.swagger.location")
    @Bean
    @Autowired
    public EnablerV2SpringFoxConfig enablerV2SpringFoxConfig(ApiPropertiesContainer apiPropertiesContainer,
                                                             DefaultListableBeanFactory beanFactory) {
        return new EnablerV2SpringFoxConfig(apiPropertiesContainer, beanFactory);
    }

}
