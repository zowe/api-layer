/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.config;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Connector;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatReactiveWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

@Order(Ordered.LOWEST_PRECEDENCE)
@Configuration
@Slf4j
public class ServerAddressPropertiesUpdater implements EnvironmentPostProcessor {

    private static final String ADDITIONAL_SUFFIX = ".additional";

    private static final Map<Integer, List<String>> ADDITIONAL_NETWORKS = new HashMap<>();

    private static String webApplicationType;

    private Properties readProperties() {
        Properties properties = new Properties();
        try (
            InputStream is = getClass().getClassLoader().getResourceAsStream("META-INF/spring.factories")
        ) {
            if (is == null) {
                log.debug("META-INF/spring.factories file was not found.");
                return properties;
            }
            properties.load(is);
        } catch (Exception e) {
            log.error("Cannot read Tomcat connector configuration", e);
        }
        return properties;
    }

    private void splitProperty(ConfigurableEnvironment environment, Map<String, Object> overriddenProperties, String addressKey, String portKey, boolean basePort) {
        String addressValue = environment.getProperty(addressKey);
        var addresses = Arrays.asList(addressValue.split(",")).stream()
            .map(String::trim)
            .toList();

        if (basePort) {
            // process the default port - the first value is configured by Spring Boot
            overriddenProperties.putIfAbsent(addressKey, addresses.get(0));
            addresses = addresses.subList(1, addresses.size());
            overriddenProperties.putIfAbsent(addressKey + ADDITIONAL_SUFFIX, StringUtils.join(addresses, ","));
        }

        if (!addresses.isEmpty()) {
            int port = Integer.parseInt(environment.getProperty(portKey));
            ADDITIONAL_NETWORKS.put(port, addresses);
        }
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        webApplicationType = environment.getProperty("spring.main.web-application-type");

        try {
            var overriddenProperties = new HashMap<String, Object>();
            var config = readProperties();
            for (int i = 0; ; i++) {
                String prefix = getClass().getName() + ".connector." + i + ".";
                if (!config.containsKey(prefix + "portKey")) break;

                splitProperty(
                    environment, overriddenProperties,
                    (String) config.get(prefix + "addressKey"),
                    (String) config.get(prefix + "portKey"),
                    Boolean.valueOf((String) config.get(prefix + "main"))
                );
            }

            if (!overriddenProperties.isEmpty()) {
                environment.getPropertySources().addFirst(new MapPropertySource("override", overriddenProperties));
            }
        } catch (RuntimeException e) {
            log.error("Cannot open additional Tomcat connectors", e);
            throw e;
        }
    }

    @Bean
    public BeanDefinitionRegistryPostProcessor registerAdditionalTomcatConnectors() {
        if (ADDITIONAL_NETWORKS.isEmpty()) {
            return registry -> {};
        }

        Class<?> connectorCustomizerClass = "servlet".equals(webApplicationType) ? AdditionalConnectorServlet.class : AdditionalConnectorReactive.class;
        return registry -> {
            for (var entry : ADDITIONAL_NETWORKS.entrySet()) {
                int port = entry.getKey();
                for (ListIterator li = entry.getValue().listIterator(); li.hasNext(); ) {
                    String address = (String) li.next();
                    String beanName = "tomcatAdditionalConnector-" + port + "-" + li.nextIndex();
                    var beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(connectorCustomizerClass)
                        .addPropertyValue("port", port)
                        .addPropertyValue("address", address)
                        .getBeanDefinition();
                    registry.registerBeanDefinition(beanName, beanDefinition);
                }
            }
        };
    }

    @RequiredArgsConstructor
    static class AdditionalConnectorReactive implements WebServerFactoryCustomizer<TomcatReactiveWebServerFactory> {

        private final List<TomcatConnectorCustomizer> connectorCustomizers;

        @Setter
        private int port;
        @Setter
        private String address;

        @Override
        public void customize(TomcatReactiveWebServerFactory factory) {
            var connector = new Connector();

            try {
                Method method = TomcatReactiveWebServerFactory.class.getDeclaredMethod("customizeConnector", Connector.class);
                method.setAccessible(true);
                method.invoke(factory, connector);
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }

            connector.setPort(port);
            if (address != null) {
                connector.setProperty("address", address);
            }

            factory.addAdditionalTomcatConnectors(connector);
            factory.addConnectorCustomizers(connectorCustomizers.toArray(new TomcatConnectorCustomizer[0]));
        }

    }

    @RequiredArgsConstructor
    static class AdditionalConnectorServlet implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

        private final List<TomcatConnectorCustomizer> connectorCustomizers;

        @Setter
        private int port;
        @Setter
        private String address;

        @Override
        public void customize(TomcatServletWebServerFactory factory) {
            var connector = new Connector();

            try {
                Method method = TomcatServletWebServerFactory.class.getDeclaredMethod("customizeConnector", Connector.class);
                method.setAccessible(true);
                method.invoke(factory, connector);
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }

            connector.setPort(port);
            if (address != null) {
                connector.setProperty("address", address);
            }

            factory.addAdditionalTomcatConnectors(connector);
            factory.addConnectorCustomizers(connectorCustomizers.toArray(new TomcatConnectorCustomizer[0]));
        }

    }

}
