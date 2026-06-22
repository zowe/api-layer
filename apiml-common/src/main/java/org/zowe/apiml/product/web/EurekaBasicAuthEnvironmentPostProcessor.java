/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.web;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.zowe.apiml.product.eureka.EurekaServiceUrlUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * When TLS validation of services is disabled ({@code apiml.security.ssl.verifySslCertificatesOfServices=false}) the
 * client certificate of a service cannot be trusted, therefore the Discovery Service requires HTTP basic authentication
 * instead of client certificate authentication. The Netflix Eureka client performs basic authentication only when the
 * credentials are present in the service URL, so this post processor embeds the configured Discovery Service
 * credentials ({@code apiml.discovery.userid} / {@code apiml.discovery.password}) into
 * {@code eureka.client.serviceUrl.defaultZone}.
 * <p>
 * When TLS validation is enabled (the default) or the credentials are not configured, the environment is left untouched.
 */
public class EurekaBasicAuthEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String VERIFY_CERTIFICATES_PROPERTY = "apiml.security.ssl.verifySslCertificatesOfServices";
    static final String EUREKA_USERID_PROPERTY = "apiml.discovery.userid";
    static final String EUREKA_PASSWORD_PROPERTY = "apiml.discovery.password";
    static final String DEFAULT_ZONE_PROPERTY = "eureka.client.serviceUrl.defaultZone";
    static final String PROPERTY_SOURCE_NAME = "apimlEurekaBasicAuth";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!"false".equalsIgnoreCase(environment.getProperty(VERIFY_CERTIFICATES_PROPERTY, "true"))) {
            return;
        }

        String userid = environment.getProperty(EUREKA_USERID_PROPERTY);
        String password = environment.getProperty(EUREKA_PASSWORD_PROPERTY);
        String defaultZone = environment.getProperty(DEFAULT_ZONE_PROPERTY);
        if (StringUtils.isAnyBlank(defaultZone, userid, password)) {
            return;
        }

        String updatedDefaultZone = Arrays.stream(defaultZone.split(","))
            .map(url -> EurekaServiceUrlUtils.addCredentials(url.trim(), userid, password))
            .collect(Collectors.joining(","));

        if (!updatedDefaultZone.equals(defaultZone)) {
            Map<String, Object> properties = new HashMap<>();
            properties.put(DEFAULT_ZONE_PROPERTY, updatedDefaultZone);
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
        }
    }

    @Override
    public int getOrder() {
        // run after the configuration data (application.yml) has been loaded so the properties are resolvable
        return Ordered.LOWEST_PRECEDENCE;
    }
}
