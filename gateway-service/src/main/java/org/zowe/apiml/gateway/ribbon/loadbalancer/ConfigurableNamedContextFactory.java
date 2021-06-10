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
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.cloud.context.named.NamedContextFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ConfigurableNamedContextFactory<T extends NamedContextFactory.Specification> extends NamedContextFactory<T> {

    private final String propertySourceName;
    private final String propertyName;
    protected final Class<?> defaultConfigType;

    protected ApplicationContext parent;
    protected Map<String, T> configurations = new ConcurrentHashMap<>();
    protected Map<String, Consumer<ConfigurableApplicationContext>> initializers = new ConcurrentHashMap<>();


    @Override
    public void setConfigurations(List<T> configurations) {
        for (T c : configurations) {
            this.configurations.put(c.getName(), c);
        }
    }

    public void addInitializer(String contextName, Consumer<ConfigurableApplicationContext> initializingConsumer) {
        this.initializers.put(contextName, initializingConsumer);
    }

    //Argument 1
    //Argument 2 and 3 => to set the config name to Environment
    public ConfigurableNamedContextFactory(Class<?> defaultConfigType, String propertySourceName, String propertyName) {
        super(defaultConfigType, propertySourceName, propertyName);
        this.defaultConfigType = defaultConfigType;
        this.propertySourceName = propertySourceName;
        this.propertyName = propertyName;
    }

    @Override
    public void setApplicationContext(ApplicationContext parent) throws BeansException {
        super.setApplicationContext(parent);
        this.parent = parent;
    }

    @Override
    protected AnnotationConfigApplicationContext createContext(String name) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        if (this.defaultConfigType != null) {
            context.register(defaultConfigType);
        }

        registerConfigurationsFromSpecifications(name, context);

        context.register(PropertyPlaceholderAutoConfiguration.class);

        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
            propertySourceName,
            Collections.<String, Object>singletonMap(this.propertyName, name)));

        if (this.parent != null) {
            context.setParent(this.parent);
            context.setClassLoader(this.parent.getClassLoader());
        }

        initializers.entrySet().stream()
            .filter(entry -> entry.getKey().equals(name))
            .forEach(entry -> entry.getValue().accept(context));

        context.refresh();
        context.setDisplayName(generateDisplayName(name));
        return context;
    }

    private void registerConfigurationsFromSpecifications(String name, AnnotationConfigApplicationContext context) {
        if (this.configurations.containsKey(name)) {
            for (Class<?> configuration : this.configurations.get(name)
                .getConfiguration()) {
                context.register(configuration);
            }
        }
        for (Entry<String, T> entry : this.configurations.entrySet()) {
            if (entry.getKey().startsWith("default.")) {
                for (Class<?> configuration : entry.getValue().getConfiguration()) {
                    context.register(configuration);
                }
            }
        }
    }
}
