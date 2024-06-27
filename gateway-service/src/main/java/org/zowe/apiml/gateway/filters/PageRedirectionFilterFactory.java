/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.routing.RoutedServices;
import org.zowe.apiml.product.routing.ServiceType;
import org.zowe.apiml.product.routing.transform.TransformService;
import org.zowe.apiml.product.routing.transform.URLTransformationException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class PageRedirectionFilterFactory extends AbstractGatewayFilterFactory<PageRedirectionFilterFactory.Config> {

    @Value("${server.attls.enabled:false}")
    private boolean isAttlsEnabled;

    private final EurekaClient eurekaClient;
    private final EurekaMetadataParser metadataParser;
    private TransformService transformService;

    // TODO: solve multiple instances, there is not necessary to have multiple
    public PageRedirectionFilterFactory(EurekaClient eurekaClient, @Qualifier("getEurekaMetadataParser") EurekaMetadataParser metadataParser, GatewayClient gatewayClient) {
        super(Config.class);
        this.eurekaClient = eurekaClient;
        this.metadataParser = metadataParser;
        this.transformService = new TransformService(gatewayClient);
    }

    private Optional<String> getNewLocationUrl(Config config, String location) {
        if (location == null) {
            return Optional.empty();
        }
        return ((List<InstanceInfo>) eurekaClient.getInstancesById(config.instanceId)).stream()
            .findAny()
            .map(serviceInstance -> {
                Map<String, String> metadata = serviceInstance.getMetadata();
                RoutedServices routes = metadataParser.parseRoutes(metadata);

                try {
                    String newUrl = transformService.transformURL(ServiceType.ALL, StringUtils.toRootLowerCase(config.serviceId), location, routes, false);
                    if (isAttlsEnabled) {
                        newUrl = UriComponentsBuilder.fromUriString(newUrl).scheme("https").build().toUriString();
                    }
                    return newUrl;
                } catch (URLTransformationException e) {
                    log.debug("The URL for the redirect {} cannot be transformed: {}", location, e.getMessage());
                    return null;
                }
            });
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> chain.filter(exchange).doOnSuccess(resp -> {
            var response = exchange.getResponse();
            if (response.getStatusCode().is3xxRedirection()) {
                getNewLocationUrl(config, response.getHeaders().getFirst(HttpHeaders.LOCATION))
                    .ifPresent(newUrl -> response.getHeaders().set(HttpHeaders.LOCATION, newUrl));
            }
        });
    }

    @Data
    public static class Config {

        private String serviceId;
        private String instanceId;

    }

}
