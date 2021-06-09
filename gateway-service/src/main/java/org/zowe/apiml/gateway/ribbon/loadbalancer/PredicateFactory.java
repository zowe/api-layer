/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.ribbon.loadbalancer;

import org.springframework.beans.BeansException;
import org.springframework.cloud.context.named.NamedContextFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PredicateFactory<T extends NamedContextFactory.Specification> extends NamedContextFactory<T> {

    protected ApplicationContext parent;

    protected Map<String, T> configurations = new ConcurrentHashMap<>();

    @Override
    public void setConfigurations(List<T> configurations) {
        for (T c : configurations) {
            this.configurations.put(c.getName(), c);
        }
    }

    //Argument 1
    //Argument 2 and 3 => to set the config name to Environment
    public PredicateFactory(String propertySourceName, String propertyName) {
        super(null, propertySourceName, propertyName);
    }

    @Override
    public void setApplicationContext(ApplicationContext parent) throws BeansException {
        super.setApplicationContext(parent);
        this.parent = parent;
    }

    @Override
    protected AnnotationConfigApplicationContext createContext(String name) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        if (this.configurations.containsKey(name)) {
            for (Class<?> configuration : this.configurations.get(name)
                .getConfiguration()) {
                context.register(configuration);
            }
        }
        for (Map.Entry<String, T> entry : this.configurations.entrySet()) {
            if (entry.getKey().startsWith("default.")) {
                for (Class<?> configuration : entry.getValue().getConfiguration()) {
                    context.register(configuration);
                }
            }
        }

        if (this.parent != null) {
            context.setParent(this.parent);
            context.setClassLoader(this.parent.getClassLoader());
        }

        context.getEnvironment().getPropertySources().addFirst(
            new MapPropertySource(
            "customProps", Collections.singletonMap("brekeke", "false")
            )
        );

        context.refresh(); // this has to be after adding props, where da beenz at?
        context.setDisplayName(generateDisplayName(name));
        return context;
    }
}
