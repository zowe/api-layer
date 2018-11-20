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

import com.ca.mfaas.security.HttpsConfig;
import com.ca.mfaas.security.HttpsFactory;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.NoSuchAlgorithmException;

import javax.annotation.PostConstruct;

@Configuration
@Slf4j
public class EurekaClientSecurityConfiguration {
    @Value("${server.ssl.protocol:TLSv1.2}")
    private String protocol;

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

    @Value("${server.ssl.keyAlias:#{null}}")
    private String keyAlias;

    @Value("${server.ssl.keyStore:#{null}}")
    private String keyStore;

    @Value("${server.ssl.keyPassword:#{null}}")
    private String keyPassword;

    @Value("${server.ssl.keyStorePassword:#{null}}")
    private String keyStorePassword;

    @Value("${server.ssl.keyStoreType:PKCS12}")
    private String keyStoreType;

    @Value("${apiml.security.verifySslCertificatesOfServices:true}")
    private boolean verifySslCertificatesOfServices;

    private EurekaJerseyClient eurekaJerseyClient;

    @PostConstruct
    public void init() {
        HttpsConfig httpsConfig = HttpsConfig.builder().keyAlias(keyAlias).protocol(protocol).keyStore(keyStore).keyPassword(keyPassword)
                .keyStorePassword(keyStorePassword).keyStoreType(keyStoreType).trustStore(trustStore)
                .trustStoreType(trustStoreType).trustStorePassword(trustStorePassword)
                .verifySslCertificatesOfServices(verifySslCertificatesOfServices).build();

        log.info("Using HTTPS configuration: {}", httpsConfig.toString());

        HttpsFactory factory = new HttpsFactory(httpsConfig);
        eurekaJerseyClient = factory.createEurekaJerseyClientBuilder(eurekaServerUrl, serviceId).build();
    }

    @Bean
    public DiscoveryClient.DiscoveryClientOptionalArgs discoveryClientOptionalArgs() throws NoSuchAlgorithmException {
        DiscoveryClient.DiscoveryClientOptionalArgs args = new DiscoveryClient.DiscoveryClientOptionalArgs();
        args.setEurekaJerseyClient(eurekaJerseyClient);
        return args;
    }
}
