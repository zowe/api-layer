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

import com.netflix.discovery.EurekaClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.product.instance.ServiceAddress;
import org.zowe.apiml.services.BasicInfoService;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class RegistryConfig {

    @Bean
    public BasicInfoService basicInfoService(EurekaClient eurekaClient, EurekaMetadataParser eurekaMetadataParser) {
        return new BasicInfoService(eurekaClient, eurekaMetadataParser);
    }

    @Bean
    public ServiceAddress gatewayServiceAddress(
        @Value("${apiml.service.externalUrl:#{null}}") String externalUrl,
        @Value("${server.attls.enabled:false}") boolean attlsEnabled,
        @Value("${server.ssl.enabled:true}") boolean sslEnabled,
        @Value("${apiml.service.hostname:localhost}") String hostname,
        @Value("${server.port}") int port
    ) throws URISyntaxException {
        if (externalUrl != null) {
            URI uri = new URI(externalUrl);
            return ServiceAddress.builder()
                .scheme(uri.getScheme())
                .hostname(uri.getHost() + ":" + uri.getPort())
                .build();
        }

        return ServiceAddress.builder()
            .scheme(attlsEnabled || sslEnabled ? "https" : "http")
            .hostname(hostname + ":" + port)
            .build();
    }

}
