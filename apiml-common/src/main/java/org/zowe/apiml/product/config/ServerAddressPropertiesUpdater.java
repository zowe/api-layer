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

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Order(Ordered.LOWEST_PRECEDENCE)
public class ServerAddressPropertiesUpdater implements EnvironmentPostProcessor {

    private static final String SERVER_ADDRESS = "server.address";
    private static final String ADDITIONAL_SUFFIX = ".additional";

    private static final String[] KEYS_TO_UPDATE = {
        SERVER_ADDRESS
    };

    private void splitProperty(ConfigurableEnvironment environment, Map<String, Object> overriddenProperties, String key) {
        environment.getPropertySources().stream()
            .filter(propertySource -> propertySource.containsProperty(key))
            .forEach(propertySource -> {
                var properties = String.valueOf(propertySource.getProperty(SERVER_ADDRESS)).split(",");
                if (properties.length > 1) {
                    overriddenProperties.putIfAbsent(key, properties[0].trim());
                    overriddenProperties.putIfAbsent(key + ADDITIONAL_SUFFIX, StringUtils.joinWith(",",
                        Arrays.asList(properties).subList(1, properties.length).stream()
                            .map(String::trim)
                            .toArray()
                    ));
                }
            });
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> overriddenProperties = new HashMap<>();
        Arrays.stream(KEYS_TO_UPDATE).forEach(key -> splitProperty(environment, overriddenProperties, key));
        if (!overriddenProperties.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("override", overriddenProperties));
        }
    }

}
