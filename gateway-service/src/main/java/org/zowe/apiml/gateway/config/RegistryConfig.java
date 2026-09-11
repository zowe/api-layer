/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.product.instance.ServiceAddress;
import org.zowe.apiml.registry.RegistryView;
import org.zowe.apiml.registry.ServiceRegistry;
import org.zowe.apiml.registry.client.RegistryClient;
import org.zowe.apiml.services.BasicInfoService;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class RegistryConfig {

    @Bean
    @ConditionalOnMissingBean
    BasicInfoService basicInfoService(RegistryView registry, EurekaMetadataParser eurekaMetadataParser) {
        return new BasicInfoService(registry, eurekaMetadataParser);
    }

    /**
     * Where to read the registry from.
     * <p>
     * Two answers, and no condition to get wrong: inside the modulith the registry is a bean in this same JVM
     * and is itself a {@link RegistryView}, so read it directly; standalone, the Gateway has a client and its
     * cached view is the best it can do. Resolved by asking rather than by
     * {@code @ConditionalOnMissingBean}, whose outcome here would depend on component-scan order.
     */
    @Bean
    @Primary
    RegistryView registryView(ObjectProvider<ServiceRegistry> localRegistry, ObjectProvider<RegistryClient> client) {
        ServiceRegistry inThisJvm = localRegistry.getIfAvailable();
        if (inThisJvm != null) {
            return inThisJvm;
        }
        return client.getObject().cache();
    }

    @Bean
    ServiceAddress gatewayServiceAddress(
        @Value("${apiml.service.externalUrl:#{null}}") String externalUrl,
        @Value("${server.attlsServer.enabled:false}") boolean serverAttlsEnabled,
        @Value("${server.attlsClient.enabled:false}") boolean clientAttlsEnabled,
        @Value("${server.ssl.enabled:true}") boolean sslEnabled,
        @Value("${apiml.service.hostname:localhost}") String hostname,
        @Value("${server.port}") int port
    ) throws URISyntaxException {
        if (externalUrl != null) {
            URI uri = new URI(externalUrl);
            return ServiceAddress.builder()
                .scheme(clientAttlsEnabled ? "http" : uri.getScheme())
                .hostname(uri.getHost() + ":" + uri.getPort())
                .build();
        }

        return ServiceAddress.builder()
            .scheme(determineScheme(serverAttlsEnabled, clientAttlsEnabled, sslEnabled))
            .hostname(hostname + ":" + port)
            .build();
    }

    private String determineScheme(
        boolean serverAttlsEnabled,
        boolean clientAttlsEnabled,
        boolean sslEnabled
    ) {
        String scheme;
        if (clientAttlsEnabled) {
            scheme = "http";
        } else {
            scheme = serverAttlsEnabled || sslEnabled ? "https" : "http";
        }

        return scheme;
    }

}
