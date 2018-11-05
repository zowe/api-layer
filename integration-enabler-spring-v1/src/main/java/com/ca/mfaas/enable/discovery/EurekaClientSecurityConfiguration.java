/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.enable.discovery;

import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClientImpl.EurekaJerseyClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.NoSuchAlgorithmException;

@Configuration
@Slf4j
public class EurekaClientSecurityConfiguration {
    @Value("${spring.application.name}")
    private String serviceId;

    @Value("${eureka.client.serviceUrl.defaultZone}")
    private String eurekaServerUrl;

    @Value("${server.ssl.trustStore:#{null}}")
    private String trustStore;

    @Value("${server.ssl.trustStorePassword:#{null}}")
    private String trustStorePassword;

    @Value("${server.ssl.trustStoreType:PKCS12}")
    private String trustStoreType;

    @Value("${server.ssl.keyStore:#{null}}")
    private String keyStore;

    @Value("${server.ssl.keyStorePassword:#{null}}")
    private String keyStorePassword;

    @Value("${server.ssl.keyStoreType:PKCS12}")
    private String keyStoreType;

    @Bean
    public DiscoveryClient.DiscoveryClientOptionalArgs discoveryClientOptionalArgs() throws NoSuchAlgorithmException {
        if (eurekaServerUrl.startsWith("http://")) {
            log.warn("Unsecure HTTP is used to connect to Discovery Service");
            return null;
        }
        else {
            log.info("Trust store to access Discovery Service: {}", trustStore);
            log.info("Key store to access Discovery Service: {}", keyStore);
            DiscoveryClient.DiscoveryClientOptionalArgs args = new DiscoveryClient.DiscoveryClientOptionalArgs();
            System.setProperty("javax.net.ssl.keyStore", keyStore);
            System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);
            System.setProperty("javax.net.ssl.keyStoreType", keyStoreType);
            System.setProperty("javax.net.ssl.trustStore", trustStore);
            System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
            System.setProperty("javax.net.ssl.trustStoreType", trustStoreType);
            EurekaJerseyClientBuilder builder = new EurekaJerseyClientBuilder();
            builder.withClientName(serviceId);
            builder.withSystemSSLConfiguration();
            builder.withMaxTotalConnections(10);
            builder.withMaxConnectionsPerHost(10);
            args.setEurekaJerseyClient(builder.build());
            return args;
        }
    }
}
