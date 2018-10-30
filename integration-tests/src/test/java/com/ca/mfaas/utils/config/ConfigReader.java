/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.utils.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

@Slf4j
public class ConfigReader {
    private ConfigReader() {}
    private static volatile EnvironmentConfiguration instance;

    public static EnvironmentConfiguration environmentConfiguration() {
        if (instance == null) {
            synchronized (ConfigReader.class) {
                if (instance == null) {
                    final String configFileName = "environment-configuration.yml";
                    ClassLoader classLoader = ClassLoader.getSystemClassLoader();
                    File configFile = new File(classLoader.getResource(configFileName).getFile());
                    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                    EnvironmentConfiguration configuration;
                    try {
                        configuration = mapper.readValue(configFile, EnvironmentConfiguration.class);
                    } catch (IOException e) {
                        log.info("Can't read service configuration from resource file, using default: http://localhost:10010");
                        GatewayServiceConfiguration gatewayServiceConfiguration
                            = new GatewayServiceConfiguration("https", "localhost", 10010, "qadba01", "auto01");
                        DiscoveryServiceConfiguration discoveryServiceConfiguration = new DiscoveryServiceConfiguration("http", "eureka", "password", "localhost", 10011, 1);
                        ApiCatalogServiceConfiguration apiCatalogServiceConfiguration = new ApiCatalogServiceConfiguration("user", "user");
                        configuration = new EnvironmentConfiguration(gatewayServiceConfiguration, discoveryServiceConfiguration, apiCatalogServiceConfiguration);
                    }

                    configuration.getGatewayServiceConfiguration().setScheme(System.getProperty("gateway.scheme", configuration.getGatewayServiceConfiguration().getScheme()));
                    configuration.getGatewayServiceConfiguration().setHost(System.getProperty("gateway.host", configuration.getGatewayServiceConfiguration().getHost()));
                    configuration.getGatewayServiceConfiguration().setPort(Integer.parseInt(System.getProperty("gateway.port", String.valueOf(configuration.getGatewayServiceConfiguration().getPort()))));
                    configuration.getGatewayServiceConfiguration().setUser(System.getProperty("gateway.user", configuration.getGatewayServiceConfiguration().getUser()));
                    configuration.getGatewayServiceConfiguration().setPassword(System.getProperty("gateway.password", configuration.getGatewayServiceConfiguration().getPassword()));
                    configuration.getDiscoveryServiceConfiguration().setScheme(System.getProperty("discovery.scheme", configuration.getDiscoveryServiceConfiguration().getScheme()));
                    configuration.getDiscoveryServiceConfiguration().setUser(System.getProperty("discovery.user", configuration.getDiscoveryServiceConfiguration().getUser()));
                    configuration.getDiscoveryServiceConfiguration().setPassword(System.getProperty("discovery.password", configuration.getDiscoveryServiceConfiguration().getPassword()));
                    configuration.getDiscoveryServiceConfiguration().setHost(System.getProperty("discovery.host", configuration.getDiscoveryServiceConfiguration().getHost()));
                    configuration.getDiscoveryServiceConfiguration().setPort(Integer.parseInt(System.getProperty("discovery.port", String.valueOf(configuration.getDiscoveryServiceConfiguration().getPort()))));
                    configuration.getDiscoveryServiceConfiguration().setInstances(Integer.parseInt(System.getProperty("discovery.instances", String.valueOf(configuration.getDiscoveryServiceConfiguration().getInstances()))));
                    configuration.getApiCatalogServiceConfiguration().setUser(System.getProperty("apicatalog.user", configuration.getApiCatalogServiceConfiguration().getUser()));
                    configuration.getApiCatalogServiceConfiguration().setPassword(System.getProperty("apicatalog.password", configuration.getApiCatalogServiceConfiguration().getPassword()));

                    instance = configuration;
                }
            }
        }

        return instance;
    }
}
