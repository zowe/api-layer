/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.context;

import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.cloud.context.named.NamedContextFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * An extension to Spring Cloud's {@link NamedContextFactory}.
 * Enables creation of exclusive named contexts.
 *
 * See the unit tests for documentation of usage.
 *
 */

public class ConfigurableNamedContextFactory<T extends NamedContextFactory.Specification> extends NamedContextFactory<T> {

    private final String propertySourceName;
    private final String propertyName;
    protected final Class<?> defaultConfigType;

    protected ApplicationContext parent;
    protected Map<String, T> configurations = new ConcurrentHashMap<>();
    protected Map<String, Consumer<ConfigurableApplicationContext>> initializers = new ConcurrentHashMap<>();


    /**
     * Register {@link NamedContextFactory.Specification} for newly created context
     * Specifications are context specific by Specification's name
     * Specifications with name `default.` are matched for every context name
     * @param configurations
     */
    @Override
    public void setConfigurations(List<T> configurations) {
        for (T c : configurations) {
            this.configurations.put(c.getName(), c);
        }
    }

    /**
     * Adds initializer to allow to customize the context before bean creation. Useful for
     * customizint the Environment abstraction.
     * Initializers are backed by {@link ConcurrentHashMap}. One initializer per named context.
     * Repeated adding of initializers will rewrite the previously set initializer.
     * @param contextName For which context name to initialize
     * @param initializingConsumer Initializing logic
     */
    public void addInitializer(String contextName, Consumer<ConfigurableApplicationContext> initializingConsumer) {
        this.initializers.put(contextName, initializingConsumer);
    }


    /**
     * May register default configuration for all contexts
     * Define the key for storing named context's name in the Environment.
     * @param defaultConfigType Registers default configuration class for all contexts
     * @param propertySourceName Specify the ConfigurationSource's name for NamedContext's name
     * @param propertyName Specify property key for NamedContext's name
     */
    public ConfigurableNamedContextFactory(@Nullable Class<?> defaultConfigType, String propertySourceName, String propertyName) {
        super(defaultConfigType, propertySourceName, propertyName);
        this.defaultConfigType = defaultConfigType;
        this.propertySourceName = propertySourceName;
        this.propertyName = propertyName;
    }

    /**
     * Set parent context of newly created named contexts
     */
    @Override
    public void setApplicationContext(ApplicationContext parent) throws BeansException {
        super.setApplicationContext(parent);
        this.parent = parent;
    }

    @Override
    public AnnotationConfigApplicationContext createContext(String name) {
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

        Consumer<ConfigurableApplicationContext> specificInitializer = initializers.get(name);
        if ( specificInitializer != null) {
            specificInitializer.accept(context);
        }

        context.refresh();
        context.setDisplayName(generateDisplayName(name));
        return context;
    }

    private void registerConfigurationsFromSpecifications(String name, AnnotationConfigApplicationContext context) {

        for (Entry<String, T> entry : this.configurations.entrySet()) {
            if (entry.getKey().startsWith("default.")) {
                for (Class<?> configuration : entry.getValue().getConfiguration()) {
                    context.register(configuration);
                }
            }
        }

        if (this.configurations.containsKey(name)) {
            for (Class<?> configuration : this.configurations.get(name)
                .getConfiguration()) {
                context.register(configuration);
            }
        }

    }
}
