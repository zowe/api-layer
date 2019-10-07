/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.product.logging;

import com.ca.mfaas.message.core.MessageService;
import com.ca.mfaas.message.log.ApimlLogger;
import com.ca.mfaas.message.yaml.YamlMessageServiceInstance;
import com.ca.mfaas.product.logging.annotations.InjectApimlLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import javax.annotation.Nonnull;

/**
 * This class serves as an injector for {@link ApimlLogger} in the Spring environment.
 * ApimlLogInjector detects whether any class has a field with the {@link InjectApimlLogger} annotation.
 * When it finds the {@link InjectApimlLogger} annotation, {@link ApimlLogger} will be initialized.
 */
@Component
@RequiredArgsConstructor
public class ApimlLogInjector implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName) {
        return bean;
    }

    @Override
    public Object postProcessBeforeInitialization(final Object bean, @Nonnull String name) {
        ReflectionUtils.doWithFields(bean.getClass(), field -> {
            // make the field accessible if defined private
            ReflectionUtils.makeAccessible(field);
            if (field.getAnnotation(InjectApimlLogger.class) != null) {
                Class clazz = getClass(bean);
                ApimlLogger log = ApimlLogger.of(clazz, YamlMessageServiceInstance.getInstance());
                field.set(bean, log);
            }
        });
        return bean;
    }

    private Class getClass(Object bean) {
        Class clazz = bean.getClass();

        String fullName = clazz.getName();
        int index = fullName.indexOf('$');
        if (index > -1) {
            String className = fullName.substring(0, index);
            try {
                clazz =  Class.forName(className);
            } catch (ClassNotFoundException e) {
                clazz = ApimlLogInjector.class;
            }
        }

        return clazz;
    }
}
