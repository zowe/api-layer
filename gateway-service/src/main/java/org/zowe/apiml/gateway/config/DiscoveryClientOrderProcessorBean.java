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

import com.netflix.discovery.EurekaClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Stream;

@Component
public class DiscoveryClientOrderProcessorBean implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        Stream.of(DiscoveryClient.class, EurekaClient.class, CloudEurekaClient.class)
            .map(beanFactory::getBeanNamesForType)
            .flatMap(Arrays::stream)
            .distinct()
            .map(beanFactory::getBeanDefinition)
            .forEach(bd -> bd.setDependsOn("gatewayLoadBalancerClientFilter", "eurekaAutoServiceRegistration"));
    }

}
