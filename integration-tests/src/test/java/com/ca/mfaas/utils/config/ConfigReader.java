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
import java.util.Arrays;

@Slf4j
public class ConfigReader {
    private ConfigReader() {
    }

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
                        log.warn("Can't read service configuration from resource file, using default: http://localhost:10010", e);
                        Credentials credentials = new Credentials("user", "user");
                        GatewayServiceConfiguration gatewayServiceConfiguration
                            = new GatewayServiceConfiguration("https", "localhost", 10010, 1);
                        DiscoveryServiceConfiguration discoveryServiceConfiguration = new DiscoveryServiceConfiguration("https", "eureka", "password", "localhost", 10011, 1);

                        TlsConfiguration tlsConfiguration = TlsConfiguration.builder()
                            .keyAlias("localhost")
                            .keyPassword("password")
                            .keyStoreType("PKCS12")
                            .keyStore("../keystore/localhost/localhost.keystore.p12")
                            .keyStorePassword("password")
                            .trustStoreType("PKCS12")
                            .trustStore("../keystore/localhost/localhost.truststore.p12")
                            .trustStorePassword("password")
                            .protocol("TLSv1.2")
                            .ciphers(
                                Arrays.asList(
                                    "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                                    "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                                    "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                                    "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                                    "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                                    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
                                    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                                    "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384"
                                )
                            )
                            .build();

                        ZosmfServiceConfiguration zosmfServiceConfiguration = new ZosmfServiceConfiguration("https", "ca32.ca.com", 1443);
                        configuration = new EnvironmentConfiguration(credentials, gatewayServiceConfiguration, discoveryServiceConfiguration, tlsConfiguration, zosmfServiceConfiguration);
                    }
                    configuration.getCredentials().setUser(System.getProperty("credentials.user", configuration.getCredentials().getUser()));
                    configuration.getCredentials().setPassword(System.getProperty("credentials.password", configuration.getCredentials().getPassword()));

                    configuration.getGatewayServiceConfiguration().setScheme(System.getProperty("gateway.scheme", configuration.getGatewayServiceConfiguration().getScheme()));
                    configuration.getGatewayServiceConfiguration().setHost(System.getProperty("gateway.host", configuration.getGatewayServiceConfiguration().getHost()));
                    configuration.getGatewayServiceConfiguration().setPort(Integer.parseInt(System.getProperty("gateway.port", String.valueOf(configuration.getGatewayServiceConfiguration().getPort()))));
                    configuration.getGatewayServiceConfiguration().setInstances(Integer.parseInt(System.getProperty("gateway.instances", String.valueOf(configuration.getGatewayServiceConfiguration().getInstances()))));

                    configuration.getDiscoveryServiceConfiguration().setScheme(System.getProperty("discovery.scheme", configuration.getDiscoveryServiceConfiguration().getScheme()));
                    configuration.getDiscoveryServiceConfiguration().setUser(System.getProperty("discovery.user", configuration.getDiscoveryServiceConfiguration().getUser()));
                    configuration.getDiscoveryServiceConfiguration().setPassword(System.getProperty("discovery.password", configuration.getDiscoveryServiceConfiguration().getPassword()));
                    configuration.getDiscoveryServiceConfiguration().setHost(System.getProperty("discovery.host", configuration.getDiscoveryServiceConfiguration().getHost()));
                    configuration.getDiscoveryServiceConfiguration().setPort(Integer.parseInt(System.getProperty("discovery.port", String.valueOf(configuration.getDiscoveryServiceConfiguration().getPort()))));
                    configuration.getDiscoveryServiceConfiguration().setInstances(Integer.parseInt(System.getProperty("discovery.instances", String.valueOf(configuration.getDiscoveryServiceConfiguration().getInstances()))));

                    setTlsConfigurationFromSystemProperties(configuration);

                    instance = configuration;
                }
            }
        }

        return instance;
    }

    private static void setTlsConfigurationFromSystemProperties(EnvironmentConfiguration configuration) {
        TlsConfiguration tlsConfiguration = configuration.getTlsConfiguration();
        tlsConfiguration.setKeyAlias(System.getProperty("tlsConfiguration.keyAlias", tlsConfiguration.getKeyAlias()));
        tlsConfiguration.setKeyPassword(System.getProperty("tlsConfiguration.keyPassword", tlsConfiguration.getKeyPassword()));
        tlsConfiguration.setKeyStoreType(System.getProperty("tlsConfiguration.keyStoreType", tlsConfiguration.getKeyStoreType()));
        tlsConfiguration.setKeyPassword(System.getProperty("tlsConfiguration.keyStorePassword", tlsConfiguration.getKeyStorePassword()));
        tlsConfiguration.setTrustStoreType(System.getProperty("tlsConfiguration.trustStoreType", tlsConfiguration.getTrustStoreType()));
        tlsConfiguration.setTrustStore(System.getProperty("tlsConfiguration.trustStore", tlsConfiguration.getTrustStore()));
        tlsConfiguration.setTrustStorePassword(System.getProperty("tlsConfiguration.trustStorePassword", tlsConfiguration.getTrustStorePassword()));
    }
}
