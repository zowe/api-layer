/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.config;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.MutableDiscoveryClientOptionalArgs;
import org.springframework.cloud.util.ProxyUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.cloudgatewayservice.service.RouteLocator;
import org.zowe.apiml.security.HttpsConfig;
import org.zowe.apiml.security.HttpsFactory;

@Configuration
@Slf4j
public class HttpConfig {
    @Value("${server.ssl.protocol:TLSv1.2}")
    private String protocol;

    @Value("${server.ssl.trustStore:#{null}}")
    private String trustStore;

    @Value("${server.ssl.trustStorePassword:#{null}}")
    private char[] trustStorePassword;

    @Value("${server.ssl.trustStoreType:PKCS12}")
    private String trustStoreType;

    @Value("${server.ssl.keyAlias:#{null}}")
    private String keyAlias;

    @Value("${server.ssl.keyStore:#{null}}")
    private String keyStore;

    @Value("${server.ssl.keyStorePassword:#{null}}")
    private char[] keyStorePassword;

    @Value("${server.ssl.keyPassword:#{null}}")
    private char[] keyPassword;

    @Value("${server.ssl.keyStoreType:PKCS12}")
    private String keyStoreType;


    @Value("${apiml.security.ssl.verifySslCertificatesOfServices:true}")
    private boolean verifySslCertificatesOfServices;

    @Value("${apiml.security.ssl.nonStrictVerifySslCertificatesOfServices:false}")
    private boolean nonStrictVerifySslCertificatesOfServices;

    @Value("${spring.application.name}")
    private String serviceId;

    @Value("${server.ssl.trustStoreRequired:false}")
    private boolean trustStoreRequired;

    @Value("${eureka.client.serviceUrl.defaultZone}")
    private String eurekaServerUrl;

    private final ApplicationContext context;

    public HttpConfig(ApplicationContext context) {
        this.context = context;
    }

    @Bean
    @Qualifier("apimlEurekaJerseyClient")
    EurekaJerseyClient getEurekaJerseyClient() {
        HttpsConfig config = HttpsConfig.builder()
            .protocol(protocol)
            .verifySslCertificatesOfServices(verifySslCertificatesOfServices)
            .nonStrictVerifySslCertificatesOfServices(nonStrictVerifySslCertificatesOfServices)
            .trustStorePassword(trustStorePassword).trustStoreRequired(trustStoreRequired)
            .trustStore(trustStore).trustStoreType(trustStoreType)
            .keyAlias(keyAlias).keyStore(keyStore).keyPassword(keyPassword)
            .keyStorePassword(keyStorePassword).keyStoreType(keyStoreType).build();
        log.info("Using HTTPS configuration: {}", config.toString());

        HttpsFactory factory = new HttpsFactory(config);
        return factory.createEurekaJerseyClientBuilder(eurekaServerUrl, serviceId).build();
    }

    @Bean(destroyMethod = "shutdown")
    @RefreshScope
    public EurekaClient eurekaClient(ApplicationInfoManager manager, EurekaClientConfig config,@Qualifier("apimlEurekaJerseyClient") EurekaJerseyClient eurekaJerseyClient,
                                     EurekaInstanceConfig instance, @Autowired(required = false) HealthCheckHandler healthCheckHandler) {
        ApplicationInfoManager appManager;
        if (AopUtils.isAopProxy(manager)) {
            appManager = ProxyUtils.getTargetObject(manager);
        } else {
            appManager = manager;
        }
        AbstractDiscoveryClientOptionalArgs<?> args = new MutableDiscoveryClientOptionalArgs();
        args.setEurekaJerseyClient(eurekaJerseyClient);

        CloudEurekaClient cloudEurekaClient = new CloudEurekaClient(appManager, config, args,
            this.context);
        cloudEurekaClient.registerHealthCheck(healthCheckHandler);
        return cloudEurekaClient;
    }


    @Bean
    public RouteLocator discoveryClientRouteDefinitionLocator(
        ReactiveDiscoveryClient discoveryClient, DiscoveryLocatorProperties properties) {
        return new RouteLocator(discoveryClient, properties);
    }
}
