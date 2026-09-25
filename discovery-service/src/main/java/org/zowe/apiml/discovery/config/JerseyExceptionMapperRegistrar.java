/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ext.ExceptionMapper;

@Component
@RequiredArgsConstructor
@SuppressWarnings("rawtypes")
public class JerseyExceptionMapperRegistrar implements BeanPostProcessor {

    private final ObjectProvider<ExceptionMapper> exceptionMappers;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof ResourceConfig) {
            ResourceConfig resourceConfig = (ResourceConfig) bean;
            exceptionMappers.forEach(resourceConfig::register);
        }
        return bean;
    }

}
