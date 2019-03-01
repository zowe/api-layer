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
import com.ca.mfaas.enable.model.ApiPropertiesContainerV1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty(prefix = "eureka.instance.metadata-map.mfaas.discovery", value = "enableApiDoc",
    havingValue = "true", matchIfMissing = true)
@Configuration
public class EnablerV1Config {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(EnablerV1Config.class);

    @ConditionalOnMissingProperty("eureka.instance.metadata-map.mfaas.api-info.swagger.location")
    @Bean
    @Autowired
    public EnablerV1SpringFoxConfig enablerV1SpringFoxConfig(ApiPropertiesContainerV1 apiPropertiesContainerV1,
                                                             DefaultListableBeanFactory beanFactory) {
        return new EnablerV1SpringFoxConfig(apiPropertiesContainerV1, beanFactory);
    }

}
